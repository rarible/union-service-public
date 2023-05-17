package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MetaContentDto.Representation
import com.rarible.protocol.union.enrichment.meta.content.ContentMetaDownloader
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

    @BeforeEach
    fun beforeEach() {
        every { itemService.blockchain } returns blockchain
        val router = BlockchainRouter(listOf(itemService), listOf(blockchain))
        downloader = ItemMetaDownloader(router, simpleHashService, contentMetaDownloader, emptyList(), contentMetaMetrics)
    }

    @Test
    fun `should enrich meta with images`() = runBlocking<Unit> {
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



}
