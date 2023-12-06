package com.rarible.protocol.union.enrichment.service

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.UnionWebClientCustomizer
import com.rarible.protocol.union.core.model.MetaSource
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.enrichment.configuration.CommonMetaProperties
import com.rarible.protocol.union.enrichment.configuration.EnrichmentMetaConfiguration
import com.rarible.protocol.union.enrichment.configuration.SimpleHash
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaMetrics
import com.rarible.protocol.union.enrichment.meta.simplehash.SimpleHashConverter
import com.rarible.protocol.union.enrichment.meta.simplehash.SimpleHashConverterService
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.model.RawMetaCache
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
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
        mapping = mapOf("ethereum" to "ethereum-goerli"),
        supported = setOf(BlockchainDto.ETHEREUM),
        supportedCollection = setOf(BlockchainDto.ETHEREUM),
    )
    private val props: CommonMetaProperties
        get() {
            val mock = mockk<CommonMetaProperties>()
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
    private val client = EnrichmentMetaConfiguration(props).simpleHashClient(customizer)
    private val metrics = ItemMetaMetrics(SimpleMeterRegistry())
    private val simpleHashConverterService = SimpleHashConverterService()
    private val collectionRepository: CollectionRepository = mockk()
    private val service = SimpleHashService(
        props = props,
        simpleHashClient = client,
        metaCacheRepository = cacheRepository,
        metrics = metrics,
        itemServiceRouter = router,
        simpleHashConverterService = simpleHashConverterService,
        collectionRepository = collectionRepository,
    )

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

    @Test
    fun `fetch collection meta - ok`() = runBlocking<Unit> {
        mockServer.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(this::class.java.getResource("/simplehash/collection.json").readText())
        )
        coEvery {
            collectionRepository.get(
                EnrichmentCollectionId(
                    BlockchainDto.ETHEREUM,
                    "0x8d9710f0e193d3f95c0723eaaf1a81030dc9116d"
                )
            )
        } returns EnrichmentCollection(
            blockchain = BlockchainDto.ETHEREUM,
            collectionId = "0x8d9710f0e193d3f95c0723eaaf1a81030dc9116d",
            name = null,
            bestSellOrders = emptyMap(),
            bestBidOrders = emptyMap(),
            lastUpdatedAt = nowMillis(),
        )

        val result =
            service.fetch(CollectionIdDto(BlockchainDto.ETHEREUM, "0x8d9710f0e193d3f95c0723eaaf1a81030dc9116d"))

        assertThat(result).isEqualTo(
            UnionCollectionMeta(
                name = "HYTOPIA Worlds",
                description = "HYTOPIA is a game and creator platform developed by Minecraft modding experts, aiming to overcome Minecraft's limitations and become \"the next Minecraft.\" The platform promotes innovation and collaboration among players, creators, and contributors, fostering an interconnected ecosystem with a new game engine and resources. HYTOPIA's mission is to create the largest UGC (user-generated content) games platform in the world.\n" +
                    "\n" +
                    "HYTOPIA is comprised of 10,000 worlds - a world is required to create, launch and monetize a massively multiplayer game within the HYTOPIA ecosystem.\n" +
                    "\n" +
                    "Learn more about HYTOPIA at: https://hytopia.com",
                content = listOf(
                    UnionMetaContent(
                        url = "https://lh3.googleusercontent.com/rwPEL9SuOYdEpeY2dwIIcRpDZt9Day4MO75zzRQCGjZHnJOm2wJ4DZ2U_rbMDPRzLm3y-93xFny7BuyC1mneVnhDEZSR1_gWfVM",
                        representation = MetaContentDto.Representation.ORIGINAL,
                    )
                ),
            )
        )
    }

    @Test
    fun `fetch collection meta custom - fail`() = runBlocking<Unit> {
        coEvery {
            collectionRepository.get(
                EnrichmentCollectionId(
                    BlockchainDto.ETHEREUM,
                    "0x8d9710f0e193d3f95c0723eaaf1a81030dc9116d"
                )
            )
        } returns EnrichmentCollection(
            blockchain = BlockchainDto.ETHEREUM,
            collectionId = "0x8d9710f0e193d3f95c0723eaaf1a81030dc9116d",
            name = null,
            bestSellOrders = emptyMap(),
            bestBidOrders = emptyMap(),
            lastUpdatedAt = nowMillis(),
            parent = EnrichmentCollectionId(BlockchainDto.ETHEREUM, "0x8d9710f0e193d3f95c0723eaaf1a81030dc9116a"),
            structure = UnionCollection.Structure.COMPOSITE,
        )

        val result =
            service.fetch(CollectionIdDto(BlockchainDto.ETHEREUM, "0x8d9710f0e193d3f95c0723eaaf1a81030dc9116d"))

        assertThat(result).isNull()
    }

    @Test
    fun `fetch collection meta not supported - fail`() = runBlocking<Unit> {
        val result =
            service.fetch(CollectionIdDto(BlockchainDto.POLYGON, "0x8d9710f0e193d3f95c0723eaaf1a81030dc9116d"))

        assertThat(result).isNull()
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
        originalMetaUri = "https://boredapeyachtclub.com/api/mutants/2691",
        source = MetaSource.SIMPLE_HASH,
    )
}
