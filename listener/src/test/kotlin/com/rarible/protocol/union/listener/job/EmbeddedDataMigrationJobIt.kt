package com.rarible.protocol.union.listener.job

import com.rarible.core.common.nowMillis
import com.rarible.core.meta.resource.model.MimeType
import com.rarible.core.meta.resource.parser.ipfs.IpfsUrlResourceParser
import com.rarible.loader.cache.internal.CacheRepository
import com.rarible.loader.cache.internal.MongoCacheEntry
import com.rarible.protocol.union.core.model.UnionAudioProperties
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionMetaContentProperties
import com.rarible.protocol.union.core.producer.UnionInternalBlockchainEventProducer
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.enrichment.configuration.EmbeddedContentProperties
import com.rarible.protocol.union.enrichment.meta.content.ContentMetaService
import com.rarible.protocol.union.enrichment.meta.content.cache.IpfsContentCache
import com.rarible.protocol.union.enrichment.meta.embedded.EmbeddedContentService
import com.rarible.protocol.union.enrichment.meta.embedded.LegacyEmbeddedContentUrlDetector
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaDownloader
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders

@IntegrationTest
class EmbeddedDataMigrationJobIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var embeddedContentService: EmbeddedContentService

    @Autowired
    lateinit var unionContentMetaService: ContentMetaService

    @Autowired
    lateinit var cacheRepository: CacheRepository

    @Autowired
    lateinit var eventProducer: UnionInternalBlockchainEventProducer

    @Autowired
    lateinit var enrichmentItemService: EnrichmentItemService

    @Autowired
    lateinit var ipfsContentCache: IpfsContentCache

    private val legacyEmbeddedContentUrlDetector = LegacyEmbeddedContentUrlDetector(
        EmbeddedContentProperties(publicUrl = "", legacyUrls = "http://embedded.com")
    )

    private lateinit var job: EmbeddedDataMigrationJob

    val dataProvider: DataProvider = mockk()

    @BeforeEach
    fun beforeEach() {
        job = EmbeddedDataMigrationJob(
            legacyEmbeddedContentUrlDetector,
            embeddedContentService,
            unionContentMetaService,
            cacheRepository,
            eventProducer,
            enrichmentItemService,
            ipfsContentCache,
            dataProvider
        )
        clearMocks(
            dataProvider
        )
    }

    val cid = "QmeqeBpsYTuJL8AZhY9fGBeTj9QuvMVqaZeRWFnjA24QEE"

    @Test
    fun `migrate legacy embedded meta`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val properties = UnionImageProperties(MimeType.SVG_XML_IMAGE.value)
        val url = "http://embedded.com/$itemId"

        val body = "<svg>!!!</svg>".toByteArray()
        val embeddedId = unionContentMetaService.getEmbeddedId(body)
        val embeddedUrl = unionContentMetaService.getEmbeddedSchemaUrl(embeddedId)
        val mimeType = MimeType.SVG_XML_IMAGE.value

        coEvery { dataProvider.getData(url, itemId.fullId()) } returns HttpData(body, contentType(mimeType))
        ethereumItemControllerApiMock.mockGetNftItemById(itemId, randomEthNftItemDto(itemId))

        val cache = createMeta(itemId, url, properties)

        job.migrate(null).collect()

        val updatedCache = getCache(itemId)!!
        val updatedMeta = updatedCache.data
        val updatedContent = updatedMeta.content[0]

        // Only URL changed for the content
        assertThat(updatedMeta.content).hasSize(1)
        assertThat(updatedContent).isEqualTo(cache.data.content[0].copy(url = embeddedUrl))
        assertThat(updatedMeta).isEqualTo(cache.data.copy(content = updatedMeta.content))

        // Embedded content saved
        val embedded = embeddedContentService.get(embeddedId)!!
        assertThat(embedded.mimeType).isEqualTo(properties.mimeType)
        assertThat(embedded.data).isEqualTo(body)

        // Ensure we tried to get Item in order to send event
        coVerify(exactly = 1) { testEthereumItemApi.getNftItemById(itemId.value) }
    }

    @Test
    fun `update legacy ipfs url - not full properties`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val properties = UnionImageProperties(MimeType.SVG_XML_IMAGE.value)
        val url = "https://rarible.mypinata.cloud/ipfs/$cid"
        val ipfsUrl = IpfsUrlResourceParser().parse("ipfs://$cid")!!

        val cache = createMeta(itemId, url, properties)

        job.migrate(null).collect()

        val updatedCache = getCache(itemId)!!
        val updatedMeta = updatedCache.data
        val updatedContent = updatedMeta.content[0]

        // Only URL changed for the content
        assertThat(updatedMeta.content).hasSize(1)
        assertThat(updatedContent).isEqualTo(cache.data.content[0].copy(url = ipfsUrl.original))
        assertThat(updatedMeta).isEqualTo(cache.data.copy(content = updatedMeta.content))

        // IPFS cache should not be saved - properties are not full
        val ipfsCache = ipfsContentCache.get(ipfsUrl)
        assertThat(ipfsCache).isNull()

        // Nothing should be sent
        coVerify(exactly = 0) { testEthereumItemApi.getNftItemById(any()) }
    }

    @Test
    fun `update legacy ipfs url - full properties`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val properties = UnionImageProperties(MimeType.SVG_XML_IMAGE.value, 100, 100, 100)
        val url = "https://rarible.mypinata.cloud/ipfs/$cid"
        val ipfsUrl = IpfsUrlResourceParser().parse("ipfs://$cid")!!

        val cache = createMeta(itemId, url, properties)

        job.migrate(null).collect()

        val updatedCache = getCache(itemId)!!
        val updatedMeta = updatedCache.data
        val updatedContent = updatedMeta.content[0]

        // Only URL changed for the content
        assertThat(updatedMeta.content).hasSize(1)
        assertThat(updatedContent).isEqualTo(cache.data.content[0].copy(url = ipfsUrl.original))
        assertThat(updatedMeta).isEqualTo(cache.data.copy(content = updatedMeta.content))

        // IPFS cache should be saved - properties are full
        val ipfsCache = ipfsContentCache.get(ipfsUrl)!!
        assertThat(ipfsCache.content).isEqualTo(properties)

        // Nothing should be sent
        coVerify(exactly = 0) { testEthereumItemApi.getNftItemById(any()) }
    }

    @Test
    fun `update cache for ipfs - not full properties`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val properties = UnionAudioProperties(MimeType.MP3_AUDIO.value, null)
        val url = "https://ipfs.io/ipfs/$cid/abc"
        val ipfsUrl = IpfsUrlResourceParser().parse(url)!!

        val cache = createMeta(itemId, url, properties)

        job.migrate(null).collect()

        val updatedCache = getCache(itemId)!!

        // Meta should not change
        assertThat(updatedCache).isEqualTo(cache)

        // IPFS cache should not be saved - properties are not full
        val ipfsCache = ipfsContentCache.get(ipfsUrl)
        assertThat(ipfsCache).isNull()

        // Nothing should be sent
        coVerify(exactly = 0) { testEthereumItemApi.getNftItemById(any()) }
    }

    @Test
    fun `update cache for ipfs - full properties`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val properties = UnionAudioProperties(MimeType.MP3_AUDIO.value, 1000)
        val url = "https://ipfs.io/ipfs/$cid/abc"
        val ipfsUrl = IpfsUrlResourceParser().parse(url)!!

        val cache = createMeta(itemId, url, properties)

        job.migrate(null).collect()

        val updatedCache = getCache(itemId)!!

        // Meta should not change
        assertThat(updatedCache).isEqualTo(cache)

        // IPFS cache should be saved - properties are full
        val ipfsCache = ipfsContentCache.get(ipfsUrl)!!
        assertThat(ipfsCache.content).isEqualTo(properties)

        // Nothing should be sent
        coVerify(exactly = 0) { testEthereumItemApi.getNftItemById(any()) }
    }

    @Test
    fun `fix embedded cache`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        // Broken url with incorrect embedded data
        val url = "http://test.com/<svg>!!!</svg>"

        val body = "<svg>!!!</svg>".toByteArray()
        val embeddedId = unionContentMetaService.getEmbeddedId(body)
        val embeddedUrl = unionContentMetaService.getEmbeddedSchemaUrl(embeddedId)

        ethereumItemControllerApiMock.mockGetNftItemById(itemId, randomEthNftItemDto(itemId))

        val cache = createMeta(itemId, url, null)

        job.migrate(null).collect()

        val updatedCache = getCache(itemId)!!
        val updatedMeta = updatedCache.data
        val updatedContent = updatedMeta.content[0]

        // Properties updated to valid embedded data
        assertThat(updatedMeta.content).hasSize(1)
        assertThat(updatedContent.url).isEqualTo(embeddedUrl)
        assertThat(updatedContent.properties).isEqualTo(
            UnionImageProperties(MimeType.SVG_XML_IMAGE.value, 14, 192, 192)
        )
        assertThat(updatedMeta).isEqualTo(cache.data.copy(content = updatedMeta.content))

        // Embedded content saved
        val embedded = embeddedContentService.get(embeddedId)!!
        assertThat(embedded.mimeType).isEqualTo(MimeType.SVG_XML_IMAGE.value)
        assertThat(embedded.data).isEqualTo(body)

        // Ensure we tried to get Item in order to send event
        coVerify(exactly = 1) { testEthereumItemApi.getNftItemById(itemId.value) }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            // regular http/https urls
            "http://something.com/abc",
            "https://something.com/abc",
            // broken url, we are not fixing them now
            "brokendata",
            // arweave
            "https://arweave.net/abc",
            // foreign ipfs url
            "https://ipfs.io/ipfs/QmeqeBpsYTuJL8AZhY9fGBeTj9QuvMVqaZeRWFnjA24QEE"
        ]
    )
    fun `skipped meta`(url: String) = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val cache = createMeta(itemId, url, null)

        job.migrate(null).collect()

        val updatedCache = getCache(itemId)!!

        // Meta should not change
        assertThat(updatedCache).isEqualTo(cache)

        coVerify(exactly = 0) { testEthereumItemApi.getNftItemById(itemId.value) }
    }

    private suspend fun createMeta(
        itemId: ItemIdDto,
        url: String,
        properties: UnionMetaContentProperties?
    ): MongoCacheEntry<UnionMeta> {
        val meta = randomUnionMeta().copy(
            content = listOf(
                UnionMetaContent(
                    url = url,
                    representation = MetaContentDto.Representation.ORIGINAL,
                    fileName = null,
                    properties = properties
                )
            )
        )
        return cacheRepository.save(
            type = ItemMetaDownloader.TYPE,
            key = itemId.fullId(),
            data = meta,
            cachedAt = nowMillis()
        )
    }

    private suspend fun getCache(itemId: ItemIdDto): MongoCacheEntry<UnionMeta>? {
        return cacheRepository.get(
            ItemMetaDownloader.TYPE,
            itemId.fullId()
        )
    }

    private fun contentType(type: String): HttpHeaders {
        val headers = HttpHeaders()
        headers.put(HttpHeaders.CONTENT_TYPE, listOf(type))
        return headers
    }
}

