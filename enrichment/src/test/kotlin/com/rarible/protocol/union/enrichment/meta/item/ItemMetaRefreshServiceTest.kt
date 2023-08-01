package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.core.test.data.randomLong
import com.rarible.protocol.union.core.model.download.PartialDownloadException
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.repository.MetaRefreshRequestRepository
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.test.data.randomEsItem
import com.rarible.protocol.union.enrichment.test.data.randomItemMetaDownloadEntry
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(MockKExtension::class)
internal class ItemMetaRefreshServiceTest {

    @InjectMockKs
    private lateinit var itemMetaRefreshService: ItemMetaRefreshService

    @MockK
    private lateinit var esItemRepository: EsItemRepository

    @MockK
    private lateinit var itemRepository: ItemRepository

    @MockK
    private lateinit var itemMetaService: ItemMetaService

    @MockK
    private lateinit var metaRefreshRequestRepository: MetaRefreshRequestRepository

    @BeforeEach
    fun beforeEach() {
        clearMocks(metaRefreshRequestRepository)
        coEvery { metaRefreshRequestRepository.save(any()) } returns Unit
    }

    @Test
    fun `collection size is small`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        coEvery { esItemRepository.countItemsInCollection(collectionId.fullId()) } returns randomLong(1000)

        assertThat(itemMetaRefreshService.runRefreshIfAllowed(collectionId, true)).isTrue()
        coVerify(exactly = 1) {
            metaRefreshRequestRepository.save(match { it.collectionId == collectionId.fullId() })
        }
    }

    @Test
    fun `collection size is too big`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        coEvery { esItemRepository.countItemsInCollection(collectionId.fullId()) } returns 40000 + randomLong(1000)

        assertThat(itemMetaRefreshService.runRefreshIfAllowed(collectionId, true)).isFalse()
        assertThat(itemMetaRefreshService.runAutoRefreshIfAllowed(collectionId, true)).isFalse()
        coVerify(exactly = 0) { metaRefreshRequestRepository.save(any()) }
    }

    @Test
    fun `attempts exceeded`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        coEvery { esItemRepository.countItemsInCollection(collectionId.fullId()) } returns 1000 + randomLong(1000)
        coEvery { metaRefreshRequestRepository.countForCollectionId(collectionId.fullId()) } returns 3

        assertThat(itemMetaRefreshService.runRefreshIfAllowed(collectionId, true)).isFalse()
    }

    @Test
    fun `already scheduled`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        coEvery { esItemRepository.countItemsInCollection(collectionId.fullId()) } returns 1000 + randomLong(1000)
        coEvery { metaRefreshRequestRepository.countForCollectionId(collectionId.fullId()) } returns 1
        coEvery { metaRefreshRequestRepository.countNotScheduledForCollectionId(collectionId.fullId()) } returns 1

        assertThat(itemMetaRefreshService.runRefreshIfAllowed(collectionId, true)).isFalse()
    }

    @Test
    fun `check random no changes`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        coEvery { esItemRepository.countItemsInCollection(collectionId.fullId()) } returns 1000 + randomLong(1000)
        coEvery { metaRefreshRequestRepository.countForCollectionId(collectionId.fullId()) } returns 2
        coEvery { metaRefreshRequestRepository.countNotScheduledForCollectionId(collectionId.fullId()) } returns 0

        val esItem1 = randomEsItem()
        val esItem2 = randomEsItem()
        val itemId1 = IdParser.parseItemId(esItem1.itemId)
        val itemId2 = IdParser.parseItemId(esItem2.itemId)
        coEvery {
            esItemRepository.getRandomItemsFromCollection(
                collectionId = collectionId.fullId(),
                size = 100
            )
        } returns listOf(esItem1, esItem2)

        val meta1 = randomUnionMeta()
        coEvery {
            itemRepository.get(ShortItemId(itemId1))
        } returns randomShortItem().copy(metaEntry = randomItemMetaDownloadEntry(data = meta1))

        val meta2 = randomUnionMeta()
        coEvery {
            itemRepository.get(ShortItemId(itemId2))
        } returns randomShortItem().copy(metaEntry = randomItemMetaDownloadEntry(data = meta2))

        coEvery {
            itemMetaService.download(itemId = itemId1, pipeline = ItemMetaPipeline.REFRESH, force = true)
        } returns null
        coEvery {
            itemMetaService.download(itemId = itemId2, pipeline = ItemMetaPipeline.REFRESH, force = true)
        } returns meta2.copy(createdAt = Instant.now())

        assertThat(itemMetaRefreshService.runRefreshIfAllowed(collectionId, true)).isFalse()
        assertThat(itemMetaRefreshService.runAutoRefreshIfAllowed(collectionId, true)).isFalse()
    }

    @Test
    fun `check random meta changed`() = runBlocking<Unit> {
        val (collectionId, itemId) = prepareData()

        coEvery {
            itemMetaService.download(itemId = itemId, pipeline = ItemMetaPipeline.REFRESH, force = true)
        } returns randomUnionMeta()

        assertThat(itemMetaRefreshService.runRefreshIfAllowed(collectionId, true)).isTrue()
        assertThat(itemMetaRefreshService.runAutoRefreshIfAllowed(collectionId, true)).isTrue()

        coVerify(exactly = 2) {
            itemMetaService.download(itemId = itemId, pipeline = ItemMetaPipeline.REFRESH, force = true)
        }
    }

    @Test
    fun `check random partial exception`() = runBlocking<Unit> {
        val (collectionId, itemId) = prepareData()

        coEvery {
            itemMetaService.download(itemId = itemId, pipeline = ItemMetaPipeline.REFRESH, force = true)
        } throws PartialDownloadException(failedProviders = emptyList(), data = randomUnionMeta())

        assertThat(itemMetaRefreshService.runRefreshIfAllowed(collectionId, true)).isTrue()
        assertThat(itemMetaRefreshService.runAutoRefreshIfAllowed(collectionId, true)).isTrue()

        coVerify(exactly = 2) {
            itemMetaService.download(itemId = itemId, pipeline = ItemMetaPipeline.REFRESH, force = true)
        }
    }

    @Test
    fun `check random exception`() = runBlocking<Unit> {
        val (collectionId, itemId) = prepareData()

        coEvery {
            itemMetaService.download(itemId = itemId, pipeline = ItemMetaPipeline.REFRESH, force = true)
        } throws RuntimeException()

        assertThat(itemMetaRefreshService.runRefreshIfAllowed(collectionId, true)).isFalse()
        assertThat(itemMetaRefreshService.runAutoRefreshIfAllowed(collectionId, true)).isFalse()

        coVerify(exactly = 2) {
            itemMetaService.download(itemId = itemId, pipeline = ItemMetaPipeline.REFRESH, force = true)
        }
    }

    private suspend fun prepareData(): Pair<CollectionIdDto, ItemIdDto> {
        val collectionId = randomEthCollectionId()
        coEvery { esItemRepository.countItemsInCollection(collectionId.fullId()) } returns 1000 + randomLong(1000)
        coEvery { metaRefreshRequestRepository.countForCollectionId(collectionId.fullId()) } returns 0
        coEvery { metaRefreshRequestRepository.countNotScheduledForCollectionId(collectionId.fullId()) } returns 0

        val esItem1 = randomEsItem()
        val esItem2 = randomEsItem()
        val itemId1 = IdParser.parseItemId(esItem1.itemId)
        val itemId2 = IdParser.parseItemId(esItem2.itemId)
        coEvery {
            esItemRepository.getRandomItemsFromCollection(
                collectionId = collectionId.fullId(),
                size = 100
            )
        } returns listOf(esItem1, esItem2)

        val meta1 = randomUnionMeta()
        coEvery {
            itemRepository.get(ShortItemId(itemId1))
        } returns randomShortItem().copy(metaEntry = randomItemMetaDownloadEntry(data = meta1))

        coEvery {
            itemRepository.get(ShortItemId(itemId2))
        } returns null

        return Pair(collectionId, itemId1)
    }
}
