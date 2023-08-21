package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.download.DownloadException
import com.rarible.protocol.union.core.model.download.MetaSource
import com.rarible.protocol.union.core.model.download.PartialDownloadException
import com.rarible.protocol.union.core.model.download.ProviderDownloadException
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MetaContentDto.Representation
import com.rarible.protocol.union.enrichment.meta.content.ContentMetaDownloader
import com.rarible.protocol.union.enrichment.meta.item.provider.DefaultItemMetaProvider
import com.rarible.protocol.union.enrichment.meta.item.provider.ItemMetaCustomProvider
import com.rarible.protocol.union.enrichment.meta.item.provider.SimpleHashItemMetaProvider
import com.rarible.protocol.union.enrichment.meta.provider.MetaCustomProvider
import com.rarible.protocol.union.enrichment.service.SimpleHashService
import com.rarible.protocol.union.enrichment.test.data.randomUnionContent
import com.rarible.protocol.union.enrichment.test.data.randomUnionImageProperties
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ItemMetaDownloaderTest {

    private val blockchain = BlockchainDto.ETHEREUM
    private val contentMetaMetrics = ItemMetaMetrics(SimpleMeterRegistry())

    private val itemService: ItemService = mockk()
    private val contentMetaDownloader: ContentMetaDownloader = mockk() {
        coEvery { enrichContent(any(), any(), any()) } returnsArgument 2
    }
    private val simpleHashService: SimpleHashService = mockk() {
        every { isSupported(blockchain) } returns true
    }
    private val customItemMetaProvider: ItemMetaCustomProvider = mockk() {
        coEvery { fetch(any(), any()) } returns MetaCustomProvider.Result(false)
    }

    private val simpleHashMetaItemProvider = SimpleHashItemMetaProvider(simpleHashService)
    private val metaContentEnrichmentService = ItemMetaContentEnrichmentService(
        contentMetaLoader = contentMetaDownloader,
        customizers = emptyList(),
    )

    private lateinit var downloader: ItemMetaDownloader
    private lateinit var defaultItemMetaProvider: DefaultItemMetaProvider

    @BeforeEach
    fun beforeEach() {
        every { itemService.blockchain } returns blockchain
        val router = BlockchainRouter(listOf(itemService), listOf(blockchain))
        defaultItemMetaProvider = DefaultItemMetaProvider(router, contentMetaMetrics)
        downloader = ItemMetaDownloader(
            router = router,
            metaContentEnrichmentService = metaContentEnrichmentService,
            providers = listOf(defaultItemMetaProvider, simpleHashMetaItemProvider),
            customProviders = listOf(customItemMetaProvider),
            metrics = contentMetaMetrics
        )
    }

    @Test
    fun `add additional image - ok`() = runBlocking<Unit> {
        val itemId = ItemIdDto(blockchain, randomString().lowercase(), randomBigInt())

        coEvery { itemService.getItemMetaById(itemId.value) } returns randomUnionMeta(
            content = listOf(UnionMetaContent(url = "ipfs://ipfs.com", representation = Representation.ORIGINAL))
        )
        coEvery { simpleHashService.fetch(itemId) } returns randomUnionMeta(
            content = listOf(UnionMetaContent(url = "https://google.com", representation = Representation.BIG))
        )

        val meta = downloader.download(itemId.toString())

        assertThat(meta.content).hasSize(2)
        assertThat(meta.source).isEqualTo(MetaSource.ORIGINAL)
        assertThat(meta.contributors).isEqualTo(listOf(MetaSource.SIMPLE_HASH))
    }

    @Test
    fun `add additional image content properties - ok, full`() = runBlocking<Unit> {
        val itemId = ItemIdDto(blockchain, randomString().lowercase(), randomBigInt())
        val properties = randomUnionImageProperties()

        coEvery { itemService.getItemMetaById(itemId.value) } returns randomUnionMeta(
            content = listOf(UnionMetaContent(url = "ipfs://ipfs.com", representation = Representation.ORIGINAL))
        )
        coEvery { simpleHashService.fetch(itemId) } returns randomUnionMeta(
            content = listOf(
                UnionMetaContent(
                    url = "ipfs://ipfs.com",
                    representation = Representation.ORIGINAL,
                    properties = properties
                )
            )
        )

        val meta = downloader.download(itemId.toString())

        assertThat(meta.content.single().properties).isEqualTo(properties)
    }

    @Test
    fun `add additional image content properties - ok, partly`() = runBlocking<Unit> {
        val itemId = ItemIdDto(blockchain, randomString().lowercase(), randomBigInt())
        val existedProperties = randomUnionImageProperties().copy(height = null, width = null)
        val extraProperties = randomUnionImageProperties()

        coEvery { itemService.getItemMetaById(itemId.value) } returns randomUnionMeta(
            content = listOf(
                UnionMetaContent(
                    url = "ipfs://ipfs.com",
                    representation = Representation.ORIGINAL,
                    properties = existedProperties
                )
            )
        )
        coEvery { simpleHashService.fetch(itemId) } returns randomUnionMeta(
            content = listOf(
                UnionMetaContent(
                    url = "ipfs://ipfs.com",
                    representation = Representation.ORIGINAL,
                    properties = extraProperties
                )
            )
        )

        val meta = downloader.download(itemId.toString())

        assertThat(meta.content.single().properties).isEqualTo(
            existedProperties.copy(width = extraProperties.width, height = extraProperties.height)
        )
    }

    @Test
    fun `do not replace image - ok`() = runBlocking<Unit> {
        val itemId = ItemIdDto(blockchain, randomString().lowercase(), randomBigInt())
        val originalImage = UnionMetaContent(url = "ipfs://ipfs.com", representation = Representation.ORIGINAL)

        coEvery { itemService.getItemMetaById(itemId.value) } returns randomUnionMeta(
            content = listOf(originalImage)
        )
        coEvery { simpleHashService.fetch(itemId) } returns randomUnionMeta(
            content = listOf(UnionMetaContent(url = "ipfs://ipfs.com/2", representation = Representation.ORIGINAL))
        )

        val meta = downloader.download(itemId.toString())

        assertThat(meta.content).hasSize(1)
        assertThat(meta.content).contains(originalImage)
    }

    @Test
    fun `add additional attribute - shouldn't add`() = runBlocking<Unit> {
        val itemId = ItemIdDto(blockchain, randomString().lowercase(), randomBigInt())

        coEvery { itemService.getItemMetaById(itemId.value) } returns randomUnionMeta().copy(
            attributes = listOf(UnionMetaAttribute("key", "value"))
        )
        coEvery { simpleHashService.fetch(itemId) } returns randomUnionMeta().copy(
            content = listOf(randomUnionContent()),
            attributes = listOf(UnionMetaAttribute("new", "value"))
        )

        val meta = downloader.download(itemId.toString())

        assertThat(meta.attributes).containsExactly(UnionMetaAttribute("key", "value"))
    }

    @Test
    fun `custom download - ok`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val meta = randomUnionMeta(content = listOf(randomUnionContent()))

        coEvery {
            customItemMetaProvider.fetch(itemId.blockchain, itemId.value)
        } returns MetaCustomProvider.Result(true, meta)

        val result = downloader.download(itemId.fullId())
        assertThat(result).isEqualTo(meta)
        coVerify(exactly = 1) { contentMetaDownloader.enrichContent(itemId.fullId(), itemId.blockchain, meta.content) }
    }

    @Test
    fun `partial download - ok, simplehash failed`() = runBlocking<Unit> {
        val itemId = randomEthItemId()

        coEvery { itemService.getItemMetaById(itemId.value) } returns randomUnionMeta().copy(
            attributes = listOf(
                UnionMetaAttribute("key", "value")
            )
        )
        coEvery { simpleHashService.fetch(itemId) } throws
            ProviderDownloadException(provider = MetaSource.SIMPLE_HASH)

        try {
            downloader.download(itemId.toString())
            fail("Shouldn't be here")
        } catch (e: PartialDownloadException) {
            val meta = e.data as UnionMeta
            assertThat(meta.attributes).hasSize(1)
            assertThat(meta.source).isEqualTo(MetaSource.ORIGINAL)
            assertThat(meta.contributors).isEqualTo(emptyList<MetaSource>())
            assertThat(e.failedProviders).containsExactly(MetaSource.SIMPLE_HASH)
        }
    }

    @Test
    fun `partial download - ok, default failed`() = runBlocking<Unit> {
        val itemId = randomEthItemId()

        coEvery { itemService.getItemMetaById(itemId.value) } throws // returns randomUnionMeta()
            ProviderDownloadException(provider = MetaSource.ORIGINAL)

        coEvery { simpleHashService.fetch(itemId) } returns randomUnionMeta(
            source = MetaSource.SIMPLE_HASH,
            content = listOf(randomUnionContent())
        )

        try {
            downloader.download(itemId.toString())
            fail("Shouldn't be here")
        } catch (e: PartialDownloadException) {
            val meta = e.data as UnionMeta
            assertThat(meta.source).isEqualTo(MetaSource.SIMPLE_HASH)
            assertThat(meta.contributors).isEqualTo(emptyList<MetaSource>())
            assertThat(e.failedProviders).containsExactly(MetaSource.ORIGINAL)
        }
    }

    @Test
    fun `download - fail`() = runBlocking<Unit> {
        val itemId = ItemIdDto(blockchain, randomString().lowercase(), randomBigInt())

        coEvery { itemService.getItemMetaById(itemId.value) } throws
            ProviderDownloadException(provider = MetaSource.ORIGINAL)

        coEvery { simpleHashService.fetch(itemId) } throws
            ProviderDownloadException(provider = MetaSource.SIMPLE_HASH)

        assertThrows<DownloadException> { downloader.download(itemId.toString()) }
    }
}
