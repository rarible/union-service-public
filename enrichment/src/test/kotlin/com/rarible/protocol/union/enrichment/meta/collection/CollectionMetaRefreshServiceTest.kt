package com.rarible.protocol.union.enrichment.meta.collection

import com.rarible.core.test.data.randomLong
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaService
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
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
    }

    @Test
    fun `check random no changes`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        whenever(esItemRepository.countItemsInCollection(collectionId.fullId())).thenReturn(1000 + randomLong(1000))

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
    }

    @Test
    fun `check random meta changed`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        whenever(esItemRepository.countItemsInCollection(collectionId.fullId())).thenReturn(1000 + randomLong(1000))

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

        whenever(
            itemMetaService.download(itemId = itemId1, pipeline = ItemMetaPipeline.REFRESH, force = true)
        ).thenReturn(randomUnionMeta())

        assertThat(collectionMetaRefreshService.shouldRefresh(collectionId)).isTrue()

        verify(itemMetaService).download(itemId = itemId1, pipeline = ItemMetaPipeline.REFRESH, force = true)
        verifyNoMoreInteractions(itemMetaService)
    }
}