package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.core.model.MetaSource
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.enrichment.download.DownloadException
import com.rarible.protocol.union.enrichment.download.DownloadStatus
import com.rarible.protocol.union.enrichment.download.ProviderDownloadException
import com.rarible.protocol.union.enrichment.meta.item.provider.ItemMetaProvider
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.test.data.randomItemMetaDownloadEntry
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class PartialItemMetaDownloaderTest {
    private lateinit var partialItemMetaDownloader: PartialItemMetaDownloader

    @MockK
    private lateinit var router: BlockchainRouter<ItemService>

    @MockK
    private lateinit var itemMetaContentEnrichmentService: ItemMetaContentEnrichmentService

    @MockK
    private lateinit var provider: ItemMetaProvider

    @MockK
    private lateinit var itemRepository: ItemRepository

    @BeforeEach
    fun before() {
        partialItemMetaDownloader = PartialItemMetaDownloader(
            router = router,
            itemMetaContentEnrichmentService = itemMetaContentEnrichmentService,
            providers = listOf(provider),
            itemRepository = itemRepository,
        )
    }

    @Test
    fun `partial load - success`() = runBlocking<Unit> {
        val itemId = randomEthItemId()

        val metaEntry = randomItemMetaDownloadEntry().copy(
            status = DownloadStatus.RETRY_PARTIAL,
            failedProviders = listOf(MetaSource.SIMPLE_HASH),
            data = randomUnionMeta(source = MetaSource.ORIGINAL)
        )
        coEvery { itemRepository.get(ShortItemId(itemId)) } returns randomShortItem().copy(metaEntry = metaEntry)

        val updatedMeta = randomUnionMeta()
        coEvery { provider.fetch(itemId.blockchain, itemId.value, metaEntry.data) } returns updatedMeta
        coEvery { provider.getSource() } returns MetaSource.SIMPLE_HASH

        val enrichedMeta = randomUnionMeta()
        coEvery { itemMetaContentEnrichmentService.enrich(itemId, updatedMeta) } returns enrichedMeta

        val result = partialItemMetaDownloader.download(itemId.fullId())

        assertThat(result).isEqualTo(enrichedMeta)
    }

    @Test
    fun `partial load - failed`() = runBlocking<Unit> {
        val itemId = randomEthItemId()

        val metaEntry = randomItemMetaDownloadEntry().copy(
            status = DownloadStatus.RETRY_PARTIAL,
            failedProviders = listOf(MetaSource.SIMPLE_HASH)
        )
        coEvery { itemRepository.get(ShortItemId(itemId)) } returns randomShortItem().copy(metaEntry = metaEntry)

        coEvery {
            provider.fetch(
                itemId.blockchain,
                itemId.value,
                metaEntry.data
            )
        } throws ProviderDownloadException(provider = MetaSource.SIMPLE_HASH)
        coEvery { provider.getSource() } returns MetaSource.SIMPLE_HASH

        assertThatExceptionOfType(DownloadException::class.java).isThrownBy {
            runBlocking {
                partialItemMetaDownloader.download(itemId.fullId())
            }
        }
    }
}
