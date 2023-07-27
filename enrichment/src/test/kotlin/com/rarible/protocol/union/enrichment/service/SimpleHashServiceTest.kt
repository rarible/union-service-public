package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.UnionWebClientCustomizer
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.enrichment.configuration.SimpleHash
import com.rarible.protocol.union.enrichment.configuration.UnionMetaConfiguration
import com.rarible.protocol.union.enrichment.configuration.UnionMetaProperties
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaMetrics
import com.rarible.protocol.union.enrichment.meta.simplehash.SimpleHashConverter
import com.rarible.protocol.union.enrichment.meta.simplehash.SimpleHashConverterService
import com.rarible.protocol.union.enrichment.model.RawMetaCache
import com.rarible.protocol.union.enrichment.repository.RawMetaCacheRepository
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.time.Instant

class SimpleHashServiceTest {

    private val mockServer = MockWebServer()
    private val simpleHashProps = SimpleHash(
        enabled = true,
        endpoint = "http://localhost:${mockServer.port}",
        mapping = mapOf("ethereum" to "ethereum-goerli")
    )
    private val props: UnionMetaProperties
        get() {
            val mock = mockk<UnionMetaProperties>()
            every { mock.simpleHash } returns simpleHashProps
            return mock
        }
    private val customizer: UnionWebClientCustomizer = mockk() {
        every { customize(any()) } returnsArgument 0
    }
    private val cacheRepository: RawMetaCacheRepository = mockk {
        coEvery { get(any<RawMetaCache.CacheId>()) } returns null
    }
    private val itemService: ItemService = mockk()
    private val router: BlockchainRouter<ItemService> = mockk() {
        every { getService(any()) } returns itemService
    }
    private val client = UnionMetaConfiguration(props).simpleHashClient(customizer)
    private val metrics = ItemMetaMetrics(SimpleMeterRegistry())
    private val simpleHashConverterService = SimpleHashConverterService()
    private val service = SimpleHashService(props, client, cacheRepository, metrics, router, simpleHashConverterService)

    @Test
    fun `get and convert item meta for item - ok`() = runBlocking<Unit> {
        val itemId = ItemIdDto(
            blockchain = BlockchainDto.ETHEREUM,
            contract = "0x60e4d786628fea6478f785a6d7e704777c86a7c6",
            tokenId = 2691.toBigInteger()
        )
        coEvery { itemService.getItemById(itemId.value) } returns randomUnionItem(itemId).copy(lazySupply = BigInteger.ZERO)

        mockServer.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(this::class.java.getResource("/simplehash/nft.json").readText())
        )

        val fetched = service.fetch(itemId)
        assertThat(fetched).isEqualTo(resourceUnionMeta)

        val request: RecordedRequest = mockServer.takeRequest()
        assertThat(request.path).isEqualTo("/nfts/ethereum-goerli/0x60e4d786628fea6478f785a6d7e704777c86a7c6/2691")
    }

    @Test
    fun `get from cache - ok`() = runBlocking<Unit> {
        val itemId = ItemIdDto(
            blockchain = BlockchainDto.ETHEREUM,
            contract = "0x60e4d786628fea6478f785a6d7e704777c86a7c6",
            tokenId = 2691.toBigInteger()
        )
        val cacheId = SimpleHashConverter.cacheId(itemId)
        val cache = RawMetaCache(
            cacheId,
            data = this::class.java.getResource("/simplehash/nft.json").readText(),
            Instant.now()
        )
        coEvery { itemService.getItemById(itemId.value) } returns randomUnionItem(itemId).copy(lazySupply = BigInteger.ZERO)
        coEvery { cacheRepository.get(cacheId) } returns cache
        val fetched = service.fetch(itemId)
        assertThat(fetched).isEqualTo(resourceUnionMeta)

        coVerify { cacheRepository.get(cacheId) }
        assertThat(mockServer.requestCount).isEqualTo(0)
    }

    @Test
    fun `get from cache - false, cache was expired`() = runBlocking<Unit> {
        val itemId = ItemIdDto(
            blockchain = BlockchainDto.ETHEREUM,
            contract = "0x60e4d786628fea6478f785a6d7e704777c86a7c6",
            tokenId = 2691.toBigInteger()
        )
        val cacheId = SimpleHashConverter.cacheId(itemId)
        val cache = RawMetaCache(
            cacheId,
            data = "",
            createdAt = Instant.now() - props.simpleHash.cacheExpiration.multipliedBy(2)
        )
        coEvery { itemService.getItemById(itemId.value) } returns randomUnionItem(itemId).copy(lazySupply = BigInteger.ZERO)
        coEvery { cacheRepository.get(cacheId) } returns cache

        mockServer.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(this::class.java.getResource("/simplehash/nft.json").readText())
        )

        service.fetch(itemId)
        assertThat(mockServer.requestCount).isEqualTo(1)
    }

    @Test
    fun `ignore getting item meta for lazy item - ok`() = runBlocking<Unit> {
        val itemId = ItemIdDto(
            blockchain = BlockchainDto.ETHEREUM,
            contract = "0x60e4d786628fea6478f785a6d7e704777c86a7c6",
            tokenId = 2691.toBigInteger()
        )
        coEvery { itemService.getItemById(itemId.value) } returns randomUnionItem(itemId).copy(lazySupply = BigInteger.ONE)

        val fetched = service.fetch(itemId)
        assertThat(fetched).isNull()

        coVerify(exactly = 1) { itemService.getItemById(itemId.value) }
        assertThat(mockServer.requestCount).isEqualTo(0)
    }

    @Test
    fun `ignore getting item meta for unsupported - ok`() = runBlocking<Unit> {
        val itemId = ItemIdDto(
            blockchain = BlockchainDto.IMMUTABLEX,
            contract = "0x60e4d786628fea6478f785a6d7e704777c86a7c6",
            tokenId = 2691.toBigInteger()
        )

        val fetched = service.fetch(itemId)
        assertThat(fetched).isNull()

        coVerify(exactly = 0) { itemService.getItemById(itemId.value) }
        assertThat(mockServer.requestCount).isEqualTo(0)
    }

    @Test
    fun `ignore getting item meta for non-existed - ok`() = runBlocking<Unit> {
        val itemId = ItemIdDto(
            blockchain = BlockchainDto.ETHEREUM,
            contract = "0x60e4d786628fea6478f785a6d7e704777c86a7c6",
            tokenId = 2691.toBigInteger()
        )
        coEvery { itemService.getItemById(itemId.value) } throws RuntimeException("not found")

        val fetched = service.fetch(itemId)
        assertThat(fetched).isNull()

        coVerify(exactly = 1) { itemService.getItemById(itemId.value) }
        assertThat(mockServer.requestCount).isEqualTo(0)
    }

    @Test
    fun `refresh contract`() = runBlocking<Unit> {
        mockServer.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("")
        )
        val collectionId = randomEthCollectionId()
        service.refreshContract(collectionId)
        val request = mockServer.takeRequest()
        assertThat(request.path).isEqualTo("/nfts/refresh/ethereum-goerli/${collectionId.value}")
        assertThat(request.method).isEqualTo("POST")
    }

    // Converted UnionMeta from test resource file '/simplehash/nft.json'
    private val resourceUnionMeta = UnionMeta(
        name = "Mutant Ape Yacht Club #2691",
        description = "The MUTANT APE YACHT CLUB is a collection of up to 20,000 Mutant Apes that can only be created by exposing an existing Bored Ape to a vial of MUTANT SERUM or by minting a Mutant Ape in the public sale.",
        createdAt = Instant.parse("2021-08-29T01:07:40Z"),
        attributes = listOf(
            UnionMetaAttribute("Background", "M1 Blue"),
            UnionMetaAttribute("Fur", "M1 Dmt"),
            UnionMetaAttribute("Eyes", "M1 Crazy"),
            UnionMetaAttribute("Clothes", "M1 Black T"),
            UnionMetaAttribute("Hat", "M1 Fisherman's Hat"),
            UnionMetaAttribute("Mouth", "M1 Bored Unshaven"),
        ),
        content = listOf(
            UnionMetaContent(
                url = "https://lh3.googleusercontent.com/Wfx7imCJSHlYEN2iXblNEc7WaR1u8PRvVdjtIfRnWmK8yHdhSwOxbBxp38Nx3d0pOPj3fTZJmP6-hGDfH96ObnL_DmlrVavfJG8=s1000",
                representation = MetaContentDto.Representation.BIG
            ),
            UnionMetaContent(
                url = "https://lh3.googleusercontent.com/Wfx7imCJSHlYEN2iXblNEc7WaR1u8PRvVdjtIfRnWmK8yHdhSwOxbBxp38Nx3d0pOPj3fTZJmP6-hGDfH96ObnL_DmlrVavfJG8=k-w1200-s2400-rj",
                representation = MetaContentDto.Representation.PORTRAIT
            ),
            UnionMetaContent(
                url = "https://lh3.googleusercontent.com/Wfx7imCJSHlYEN2iXblNEc7WaR1u8PRvVdjtIfRnWmK8yHdhSwOxbBxp38Nx3d0pOPj3fTZJmP6-hGDfH96ObnL_DmlrVavfJG8=s250",
                representation = MetaContentDto.Representation.PREVIEW
            ),
            UnionMetaContent(
                url = "ipfs://QmPkMSNK297yMrp73HMFisjjPnPzuvzgoEQM5UDPyTw1KQ",
                representation = MetaContentDto.Representation.ORIGINAL,
                properties = UnionImageProperties(
                    mimeType = "image/png",
                    size = 674801,
                    width = 1262,
                    height = 1262,
                )
            )
        ),
        restrictions = emptyList(),
        originalMetaUri = "https://boredapeyachtclub.com/api/mutants/2691"
    )
}
