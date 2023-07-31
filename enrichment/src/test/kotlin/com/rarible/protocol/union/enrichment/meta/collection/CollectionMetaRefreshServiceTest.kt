package com.rarible.protocol.union.enrichment.meta.collection

import com.rarible.core.test.data.randomLong
import com.rarible.protocol.union.core.model.download.PartialDownloadException
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaService
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.repository.MetaRefreshRequestRepository
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.test.data.randomEsItem
import com.rarible.protocol.union.enrichment.test.data.randomItemMetaDownloadEntry
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.time.Instant

@ExtendWith(MockitoExtension::class)
internal class CollectionMetaRefreshServiceTest {
    @InjectMocks
    private lateinit var collectionMetaRefreshService: CollectionMetaRefreshService

    @Mock
    private lateinit var esItemRepository: EsItemRepository

    @Mock
    private lateinit var itemRepository: ItemRepository

    @Mock
    private lateinit var itemMetaService: ItemMetaService

    @Mock
    private lateinit var metaRefreshRequestRepository: MetaRefreshRequestRepository

    @Test
    fun `collection size is small`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        whenever(esItemRepository.countItemsInCollection(collectionId.fullId())).thenReturn(randomLong(1000))

        assertThat(collectionMetaRefreshService.shouldRefresh(collectionId)).isTrue()
    }

    @Test
    fun `collection size is too big`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        whenever(esItemRepository.countItemsInCollection(collectionId.fullId())).thenReturn(40000 + randomLong(1000))

        assertThat(collectionMetaRefreshService.shouldRefresh(collectionId)).isFalse()
        assertThat(collectionMetaRefreshService.shouldAutoRefresh(collectionId.fullId())).isFalse()
    }

    @Test
    fun `attempts exceeded`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        whenever(esItemRepository.countItemsInCollection(collectionId.fullId())).thenReturn(1000 + randomLong(1000))
        whenever(metaRefreshRequestRepository.countForCollectionId(collectionId.fullId())).thenReturn(3)

        assertThat(collectionMetaRefreshService.shouldRefresh(collectionId)).isFalse()
    }

    @Test
    fun `already scheduled`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        whenever(esItemRepository.countItemsInCollection(collectionId.fullId())).thenReturn(1000 + randomLong(1000))
        whenever(metaRefreshRequestRepository.countForCollectionId(collectionId.fullId())).thenReturn(1)
        whenever(metaRefreshRequestRepository.countNotScheduledForCollectionId(collectionId.fullId()))
            .thenReturn(1)

        assertThat(collectionMetaRefreshService.shouldRefresh(collectionId)).isFalse()
    }

    @Test
    fun `check random no changes`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        whenever(esItemRepository.countItemsInCollection(collectionId.fullId())).thenReturn(1000 + randomLong(1000))
        whenever(metaRefreshRequestRepository.countForCollectionId(collectionId.fullId())).thenReturn(2)
        whenever(metaRefreshRequestRepository.countNotScheduledForCollectionId(collectionId.fullId()))
            .thenReturn(0)

        val esItem1 = randomEsItem()
        val esItem2 = randomEsItem()
        val itemId1 = IdParser.parseItemId(esItem1.itemId)
        val itemId2 = IdParser.parseItemId(esItem1.itemId)
        whenever(
            esItemRepository.getRandomItemsFromCollection(
                collectionId = collectionId.fullId(),
                size = 100
            )
        ).thenReturn(listOf(esItem1, esItem2))

        val meta1 = randomUnionMeta()
        whenever(itemRepository.get(ShortItemId(itemId1))).thenReturn(
            randomShortItem().copy(metaEntry = randomItemMetaDownloadEntry(data = meta1))
        )
        val meta2 = randomUnionMeta()
        whenever(itemRepository.get(ShortItemId(itemId2))).thenReturn(
            randomShortItem().copy(metaEntry = randomItemMetaDownloadEntry(data = meta2))
        )

        whenever(
            itemMetaService.download(itemId = itemId1, pipeline = ItemMetaPipeline.REFRESH, force = true)
        ).thenReturn(null)
        whenever(
            itemMetaService.download(itemId = itemId2, pipeline = ItemMetaPipeline.REFRESH, force = true)
        ).thenReturn(meta2.copy(createdAt = Instant.now()))

        assertThat(collectionMetaRefreshService.shouldRefresh(collectionId)).isFalse()
        assertThat(collectionMetaRefreshService.shouldAutoRefresh(collectionId.fullId())).isFalse()
    }

    @Test
    fun `check random meta changed`() = runBlocking<Unit> {
        val (collectionId, itemId1) = prepareData()

        whenever(
            itemMetaService.download(itemId = itemId1, pipeline = ItemMetaPipeline.REFRESH, force = true)
        ).thenReturn(randomUnionMeta())

        assertThat(collectionMetaRefreshService.shouldRefresh(collectionId)).isTrue()
        assertThat(collectionMetaRefreshService.shouldAutoRefresh(collectionId.fullId())).isTrue()

        verify(itemMetaService, times(2)).download(itemId = itemId1, pipeline = ItemMetaPipeline.REFRESH, force = true)
        verifyNoMoreInteractions(itemMetaService)
    }

    @Test
    fun `check random partial exception`() = runBlocking<Unit> {
        val (collectionId, itemId1) = prepareData()

        whenever(
            itemMetaService.download(itemId = itemId1, pipeline = ItemMetaPipeline.REFRESH, force = true)
        ).thenThrow(PartialDownloadException(failedProviders = emptyList(), data = randomUnionMeta()))

        assertThat(collectionMetaRefreshService.shouldRefresh(collectionId)).isTrue()
        assertThat(collectionMetaRefreshService.shouldAutoRefresh(collectionId.fullId())).isTrue()

        verify(itemMetaService, times(2)).download(itemId = itemId1, pipeline = ItemMetaPipeline.REFRESH, force = true)
        verifyNoMoreInteractions(itemMetaService)
    }

    @Test
    fun `check random exception`() = runBlocking<Unit> {
        val (collectionId, itemId1) = prepareData()

        whenever(
            itemMetaService.download(itemId = itemId1, pipeline = ItemMetaPipeline.REFRESH, force = true)
        ).thenThrow(RuntimeException())

        assertThat(collectionMetaRefreshService.shouldRefresh(collectionId)).isFalse()
        assertThat(collectionMetaRefreshService.shouldAutoRefresh(collectionId.fullId())).isFalse()

        verify(itemMetaService, times(2)).download(itemId = itemId1, pipeline = ItemMetaPipeline.REFRESH, force = true)
        verifyNoMoreInteractions(itemMetaService)
    }

    private suspend fun prepareData(): Pair<CollectionIdDto, ItemIdDto> {
        val collectionId = randomEthCollectionId()
        whenever(esItemRepository.countItemsInCollection(collectionId.fullId())).thenReturn(1000 + randomLong(1000))
        whenever(metaRefreshRequestRepository.countForCollectionId(collectionId.fullId())).thenReturn(0)
        whenever(metaRefreshRequestRepository.countNotScheduledForCollectionId(collectionId.fullId()))
            .thenReturn(0)

        val esItem1 = randomEsItem()
        val esItem2 = randomEsItem()
        val itemId1 = IdParser.parseItemId(esItem1.itemId)
        whenever(
            esItemRepository.getRandomItemsFromCollection(
                collectionId = collectionId.fullId(),
                size = 100
            )
        ).thenReturn(listOf(esItem1, esItem2))

        val meta1 = randomUnionMeta()
        whenever(itemRepository.get(ShortItemId(itemId1))).thenReturn(
            randomShortItem().copy(metaEntry = randomItemMetaDownloadEntry(data = meta1))
        )
        return Pair(collectionId, itemId1)
    }
}
