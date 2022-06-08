package com.rarible.protocol.union.listener.meta

import com.rarible.core.meta.resource.model.ContentMeta
import com.rarible.core.meta.resource.model.MimeType
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.protocol.union.core.model.UnionAudioProperties
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionUnknownProperties
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.enrichment.meta.UnionContentMetaLoader
import com.rarible.protocol.union.enrichment.meta.UnionContentMetaService
import com.rarible.protocol.union.enrichment.meta.embedded.EmbeddedContentService
import com.rarible.protocol.union.enrichment.test.data.randomUnionContent
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.apache.commons.codec.binary.Base64
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.io.IOException
import java.net.URL

@IntegrationTest
class UnionContentMetaLoaderIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var unionMetaConLoader: UnionContentMetaLoader

    @Autowired
    lateinit var unionContentMetaService: UnionContentMetaService

    @Autowired
    lateinit var embeddedContentService: EmbeddedContentService

    private val itemId = randomEthItemId()

    @Test
    fun `url not resolved`() = runBlocking<Unit> {
        val content = randomUnionContent(UnionImageProperties(MimeType.JPEG_IMAGE.value))
            .copy(url = "abc")

        val result = unionMetaConLoader.enrichContent(itemId, listOf(content))[0]

        // Nothing to do here, kept as is
        assertThat(result).isEqualTo(content)
    }

    @Test
    fun `downloaded - ok`() = runBlocking<Unit> {
        val content = randomUnionContent(UnionImageProperties())
        val contentMeta = randomContentMeta(MimeType.PNG_IMAGE.value)

        coEvery { testContentMetaReceiver.receive(URL(content.url)) } returns contentMeta

        val result = unionMetaConLoader.enrichContent(itemId, listOf(content))[0]
        val enriched = result.properties!! as UnionImageProperties

        assertThat(enriched.size).isEqualTo(contentMeta.size)
        assertThat(enriched.width).isEqualTo(contentMeta.width)
        assertThat(enriched.height).isEqualTo(contentMeta.height)
        assertThat(enriched.mimeType).isEqualTo(contentMeta.mimeType)
    }

    @Test
    fun `downloaded - type changed`() = runBlocking<Unit> {
        // specified as video, but in fact - image
        val content = randomUnionContent(UnionVideoProperties())
        val contentMeta = randomContentMeta(MimeType.PNG_IMAGE.value)

        coEvery { testContentMetaReceiver.receive(URL(content.url)) } returns contentMeta

        val result = unionMetaConLoader.enrichContent(itemId, listOf(content))[0]
        val enriched = result.properties!! as UnionImageProperties

        assertThat(enriched.size).isEqualTo(contentMeta.size)
        assertThat(enriched.width).isEqualTo(contentMeta.width)
        assertThat(enriched.height).isEqualTo(contentMeta.height)
        assertThat(enriched.mimeType).isEqualTo(contentMeta.mimeType)
    }

    @Test
    fun `downloaded - failed`() = runBlocking<Unit> {
        val input = UnionImageProperties(MimeType.PNG_IMAGE.value)
        val content = randomUnionContent(input)

        // failed with exception
        coEvery { testContentMetaReceiver.receive(content.url) } throws IOException()

        val result = unionMetaConLoader.enrichContent(itemId, listOf(content))[0]
        val enriched = result.properties as UnionImageProperties

        assertThat(enriched).isEqualTo(input)
    }

    @Test
    fun `downloaded - failed with input properties`() = runBlocking<Unit> {
        val input = UnionImageProperties(MimeType.PNG_IMAGE.value, randomLong(), randomInt(), randomInt())
        val content = randomUnionContent(input)

        coEvery { testContentMetaReceiver.receive(content.url) } returns null

        val result = unionMetaConLoader.enrichContent(itemId, listOf(content))[0]
        val enriched = result.properties as UnionImageProperties

        assertThat(enriched).isEqualTo(input)
    }

    @Test
    fun `downloaded - failed without input properties`() = runBlocking<Unit> {
        val content = randomUnionContent()

        // null returned without exception
        coEvery { testContentMetaReceiver.receive(content.url) } returns null

        val result = unionMetaConLoader.enrichContent(itemId, listOf(content))[0]
        val enriched = result.properties

        assertThat(enriched).isInstanceOf(UnionUnknownProperties::class.java)
        // By default, we suggest it as image
        assertThat(result).isEqualTo(content.copy(properties = UnionUnknownProperties()))
    }

    @Test
    fun `downloaded - url replaced`() = runBlocking<Unit> {
        // CID should be replaced with abstract IPFS url
        val cid = "QmeqeBpsYTuJL8AZhY9fGBeTj9QuvMVqaZeRWFnjA24QEE/abc"
        val content = randomUnionContent(UnionImageProperties()).copy(url = cid)
        val contentMeta = randomContentMeta(MimeType.PNG_IMAGE.value)

        coEvery { testContentMetaReceiver.receive(content.url) } returns contentMeta

        val result = unionMetaConLoader.enrichContent(itemId, listOf(content))[0]

        assertThat(result.url).isEqualTo("ipfs://$cid")
    }

    @Test
    fun `downloaded - url is not replaced`() = runBlocking<Unit> {
        // IPFS url with gateway, should not be replaced by abstract IPFS url
        val url = "https://ipfs.io/ipfs/QmeqeBpsYTuJL8AZhY9fGBeTj9QuvMVqaZeRWFnjA24QEE"
        val content = randomUnionContent(UnionImageProperties()).copy(url = url)
        val contentMeta = randomContentMeta(MimeType.PNG_IMAGE.value)

        coEvery { testContentMetaReceiver.receive(content.url) } returns contentMeta

        val result = unionMetaConLoader.enrichContent(itemId, listOf(content))[0]

        assertThat(result.url).isEqualTo(url)
    }

    @Test
    fun `embedded - ok`() = runBlocking<Unit> {
        val data = "<svg></svg>"
        val embeddedId = unionContentMetaService.getEmbeddedId(data.toByteArray())
        val content = randomUnionContent(UnionAudioProperties()).copy(url = data)

        val result = unionMetaConLoader.enrichContent(itemId, listOf(content))[0]
        val enriched = result.properties!! as UnionImageProperties

        assertThat(enriched.width).isEqualTo(192)
        assertThat(enriched.height).isEqualTo(192)
        assertThat(enriched.mimeType).isEqualTo(MimeType.SVG_XML_IMAGE.value)

        val embedded = embeddedContentService.get(embeddedId)!!
        assertThat(embedded.data).isEqualTo(data.toByteArray())
        assertThat(embedded.mimeType).isEqualTo(MimeType.SVG_XML_IMAGE.value)
    }

    @Test
    fun `embedded - ok, mime type not resolved`() = runBlocking<Unit> {
        // mime type can't be retrieved from this base64, should be taken from input properties
        val text = "abc"
        val encoded = Base64.encodeBase64String(text.toByteArray())
        val data = "some useless text;base64,$encoded"
        val embeddedId = unionContentMetaService.getEmbeddedId(text.toByteArray())
        val content = randomUnionContent(UnionImageProperties(MimeType.GIF_IMAGE.value)).copy(url = data)

        val result = unionMetaConLoader.enrichContent(itemId, listOf(content))[0]
        val enriched = result.properties!! as UnionImageProperties

        assertThat(enriched.mimeType).isEqualTo(MimeType.GIF_IMAGE.value)

        val embedded = embeddedContentService.get(embeddedId)!!
        assertThat(embedded.data).isEqualTo(text.toByteArray())
        assertThat(embedded.mimeType).isEqualTo(MimeType.GIF_IMAGE.value)
    }

    private fun randomContentMeta(mimeType: String): ContentMeta {
        return ContentMeta(
            mimeType = mimeType,
            width = randomInt(1000),
            height = randomInt(1000),
            size = randomLong()
        )
    }

}