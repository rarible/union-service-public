package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MetaContentDto.Representation
import com.rarible.protocol.union.enrichment.meta.content.ContentMetaDownloader
import com.rarible.protocol.union.enrichment.meta.item.customizer.SimpleHashMetaCustomizer
import com.rarible.protocol.union.enrichment.meta.provider.SimpleHashItemProvider
import com.rarible.protocol.union.enrichment.service.SimpleHashService
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ItemMetaDownloaderTest {

    private val blockchain = BlockchainDto.ETHEREUM
    private val itemService: ItemService = mockk()
    private val contentMetaDownloader: ContentMetaDownloader = mockk() {
        coEvery { enrichContent(any(), any(), any()) } returnsArgument 2
    }
    private lateinit var downloader: ItemMetaDownloader
    private val meterRegistry: MeterRegistry = SimpleMeterRegistry()
    private val contentMetaMetrics = ItemMetaMetrics(meterRegistry)
    private val simpleHashService: SimpleHashService = mockk() {
        every { isSupported(blockchain) } returns true
    }
    private val simpleHashMetaCustomizer = SimpleHashMetaCustomizer(contentMetaDownloader, simpleHashService)
    private val simpleHashMetaItemProvider = SimpleHashItemProvider(simpleHashService)

    @BeforeEach
    fun beforeEach() {
        every { itemService.blockchain } returns blockchain
        val router = BlockchainRouter(listOf(itemService), listOf(blockchain))
        downloader = ItemMetaDownloader(router, contentMetaDownloader,
            listOf(simpleHashMetaCustomizer), listOf(simpleHashMetaItemProvider),
            contentMetaMetrics)
    }

    @Test
    fun `add additional image - ok`() = runBlocking<Unit> {
        val itemId = ItemIdDto(blockchain, randomString().lowercase(), randomBigInt())

        coEvery { itemService.getItemMetaById(itemId.value) } returns randomUnionMeta(content = listOf(
            UnionMetaContent(url = "ipfs://ipfs.com", representation = Representation.ORIGINAL)
        ))
        coEvery { simpleHashService.fetch(itemId) } returns randomUnionMeta(content = listOf(
            UnionMetaContent(url = "https://googleusercontent.com", representation = Representation.BIG)
        ))

        val meta = downloader.download(itemId.toString())

        assertThat(meta.content).hasSize(2)
    }

    @Test
    fun `add additional attribute - ok`() = runBlocking<Unit> {
        val itemId = ItemIdDto(blockchain, randomString().lowercase(), randomBigInt())

        coEvery { itemService.getItemMetaById(itemId.value) } returns randomUnionMeta().copy(attributes = listOf(
            UnionMetaAttribute("key", "value")
        ))
        coEvery { simpleHashService.fetch(itemId) } returns randomUnionMeta().copy(attributes = listOf(
            UnionMetaAttribute("new", "value")
        ))

        val meta = downloader.download(itemId.toString())

        assertThat(meta.attributes).hasSize(2)
    }
}
