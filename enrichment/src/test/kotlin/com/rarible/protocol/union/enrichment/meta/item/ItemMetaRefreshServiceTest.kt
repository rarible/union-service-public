package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.core.test.data.randomLong
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.configuration.EnrichmentProperties
import com.rarible.protocol.union.enrichment.download.PartialDownloadException
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.repository.MetaRefreshRequestRepository
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.test.data.randomEsItem
import com.rarible.protocol.union.enrichment.test.data.randomItemMetaDownloadEntry
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
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

    @MockK
    private lateinit var enrichmentItemService: EnrichmentItemService

    @SpyK
    private var defaultItemMetaComparator = DefaultItemMetaComparator()

    @SpyK
    private var strictItemMetaComparator = StrictItemMetaComparator(defaultItemMetaComparator)

    @MockK
    private lateinit var ff: FeatureFlagsProperties

    @SpyK
    private var enrichmentProperties: EnrichmentProperties = EnrichmentProperties()

    @BeforeEach
    fun beforeEach() {
        clearMocks(metaRefreshRequestRepository, enrichmentItemService, ff)
        coEvery { metaRefreshRequestRepository.save(any()) } returns Unit
        coEvery { ff.enableCollectionAutoReveal } returns true
        coEvery { ff.enableStrictMetaComparison } returns true
    }

    @Test
    fun `run refresh - ok, small collection`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        coEvery { esItemRepository.countItemsInCollection(collectionId.fullId()) } returns randomLong(1000)

        assertThat(itemMetaRefreshService.scheduleUserRefresh(collectionId, true)).isTrue()
        coVerify(exactly = 1) {
            metaRefreshRequestRepository.save(match { it.collectionId == collectionId.fullId() })
        }
    }

    @Test
    fun `run refresh - fail, big collection`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        coEvery { esItemRepository.countItemsInCollection(collectionId.fullId()) } returns 40000 + randomLong(1000)

        assertThat(itemMetaRefreshService.scheduleUserRefresh(collectionId, true)).isFalse()
        assertThat(itemMetaRefreshService.scheduleAutoRefresh(collectionId, true)).isFalse()
        coVerify(exactly = 0) { metaRefreshRequestRepository.save(any()) }
    }

    @Test
    fun `run refresh - fail, too many attempts`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        coEvery { esItemRepository.countItemsInCollection(collectionId.fullId()) } returns 1000 + randomLong(1000)
        coEvery { metaRefreshRequestRepository.countForCollectionId(collectionId.fullId()) } returns 3

        assertThat(itemMetaRefreshService.scheduleUserRefresh(collectionId, true)).isFalse()
    }

    @Test
    fun `run refresh - fail, already in progress`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        coEvery { esItemRepository.countItemsInCollection(collectionId.fullId()) } returns 1000 + randomLong(1000)
        coEvery { metaRefreshRequestRepository.countForCollectionId(collectionId.fullId()) } returns 1
        coEvery { metaRefreshRequestRepository.countNotScheduledForCollectionId(collectionId.fullId()) } returns 1

        assertThat(itemMetaRefreshService.scheduleUserRefresh(collectionId, true)).isFalse()
    }

    @Test
    fun `run refresh and auto refresh - fail, no meta changes`() = runBlocking<Unit> {
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
                size = 10
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

        assertThat(itemMetaRefreshService.scheduleAutoRefresh(collectionId, true)).isFalse()
    }

    @Test
    fun `run refresh and auto refresh - ok, meta has been changed`() = runBlocking<Unit> {
        val (collectionId, itemId) = prepareData()

        coEvery {
            itemMetaService.download(itemId = itemId, pipeline = ItemMetaPipeline.REFRESH, force = true)
        } returns randomUnionMeta()

        assertThat(itemMetaRefreshService.scheduleUserRefresh(collectionId, true)).isTrue()
        assertThat(itemMetaRefreshService.scheduleAutoRefresh(collectionId, true)).isTrue()

        coVerify(exactly = 1) {
            itemMetaService.download(itemId = itemId, pipeline = ItemMetaPipeline.REFRESH, force = true)
        }
    }

    @Test
    fun `run refresh and auto refresh - ok, partial refresh`() = runBlocking<Unit> {
        val (collectionId, itemId) = prepareData()

        coEvery {
            itemMetaService.download(itemId = itemId, pipeline = ItemMetaPipeline.REFRESH, force = true)
        } throws PartialDownloadException(failedProviders = emptyList(), data = randomUnionMeta())

        assertThat(itemMetaRefreshService.scheduleUserRefresh(collectionId, true)).isTrue()
        assertThat(itemMetaRefreshService.scheduleAutoRefresh(collectionId, true)).isTrue()

        coVerify(exactly = 1) {
            itemMetaService.download(itemId = itemId, pipeline = ItemMetaPipeline.REFRESH, force = true)
        }
    }

    @Test
    fun `run refresh and auto refresh - fail, unexpected exception`() = runBlocking<Unit> {
        val (collectionId, itemId) = prepareData()

        coEvery {
            itemMetaService.download(itemId = itemId, pipeline = ItemMetaPipeline.REFRESH, force = true)
        } throws RuntimeException()

        assertThat(itemMetaRefreshService.scheduleUserRefresh(collectionId, true)).isTrue()
        assertThat(itemMetaRefreshService.scheduleAutoRefresh(collectionId, true)).isFalse()

        coVerify(exactly = 1) {
            itemMetaService.download(itemId = itemId, pipeline = ItemMetaPipeline.REFRESH, force = true)
        }
    }

    @Test
    fun `run refresh on meta change - ok`() = runBlocking<Unit> {
        val (collectionId, itemId) = prepareData()
        val meta1 = randomUnionMeta()
        val meta2 = randomUnionMeta()

        coEvery { enrichmentItemService.getItemCollection(ShortItemId(itemId)) } returns collectionId

        assertThat(itemMetaRefreshService.scheduleAutoRefreshOnItemMetaChanged(itemId, meta1, meta2, true)).isTrue()
    }

    @Test
    fun `run refresh on meta change - fail, meta hasn't been changed`() = runBlocking<Unit> {
        val (collectionId, itemId) = prepareData()
        val meta1 = randomUnionMeta()

        coEvery { enrichmentItemService.getItemCollection(ShortItemId(itemId)) } returns collectionId
        assertThat(itemMetaRefreshService.scheduleAutoRefreshOnItemMetaChanged(itemId, meta1, meta1, true)).isFalse()
    }

    @Test
    fun `run refresh on meta change - fail, collection is too big`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        val itemId = randomEthItemId()
        val meta1 = randomUnionMeta()
        val meta2 = randomUnionMeta()

        coEvery { enrichmentItemService.getItemCollection(ShortItemId(itemId)) } returns collectionId
        coEvery { esItemRepository.countItemsInCollection(collectionId.fullId()) } returns 40000 + randomLong(1000)

        assertThat(itemMetaRefreshService.scheduleAutoRefreshOnItemMetaChanged(itemId, meta1, meta2, true)).isFalse()
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
                size = 10
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
