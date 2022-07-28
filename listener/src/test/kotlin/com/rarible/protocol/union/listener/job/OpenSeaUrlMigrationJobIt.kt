package com.rarible.protocol.union.listener.job

import com.rarible.core.common.nowMillis
import com.rarible.core.meta.resource.model.MimeType
import com.rarible.loader.cache.internal.CacheRepository
import com.rarible.loader.cache.internal.MongoCacheEntry
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionMetaContentProperties
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaDownloader
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class OpenSeaUrlMigrationJobIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var cacheRepository: CacheRepository

    @Autowired
    lateinit var job: OpenSeaUrlMigrationJob

    @Test
    fun `legacy opensea url updated`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val properties = UnionImageProperties(MimeType.SVG_XML_IMAGE.value)
        val url = "https://storage.opensea.io/files/$itemId"
        val expectedUrl = "https://openseauserdata.com/files/$itemId"

        ethereumItemControllerApiMock.mockGetNftItemById(itemId, randomEthNftItemDto(itemId))

        val cache = createMeta(itemId, url, properties)

        job.migrate(null).collect()

        val updatedCache = getCache(itemId)!!
        val updatedMeta = updatedCache.data
        val updatedContent = updatedMeta.content[0]

        // Only URL changed for the content
        assertThat(updatedMeta.content).hasSize(1)
        assertThat(updatedContent).isEqualTo(cache.data.content[0].copy(url = expectedUrl))
        assertThat(updatedMeta).isEqualTo(cache.data.copy(content = updatedMeta.content))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "http://something.com/abc",
            "https://openseauserdata.com/files/abc",
            "https://ipfs.io/ipfs/QmeqeBpsYTuJL8AZhY9fGBeTj9QuvMVqaZeRWFnjA24QEE",
            "ipfs://QmeqeBpsYTuJL8AZhY9fGBeTj9QuvMVqaZeRWFnjA24QEE",
            "embedded://abcjvhbdfj"
        ]
    )
    fun `non-opensea urls not changed`(url: String) = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val properties = UnionImageProperties(MimeType.SVG_XML_IMAGE.value)

        val cache = createMeta(itemId, url, properties)

        job.migrate(null).collect()

        val updatedCache = getCache(itemId)!!

        // Nothing changed
        assertThat(updatedCache).isEqualTo(cache)
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

}