package com.rarible.protocol.union.api.controller

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.enrichment.meta.embedded.EmbeddedContentStorage
import com.rarible.protocol.union.enrichment.meta.embedded.UnionEmbeddedContent
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders

@IntegrationTest
class ContentControllerFt : AbstractIntegrationTest() {

    @Autowired
    lateinit var embeddedContentStorage: EmbeddedContentStorage

    @Test
    fun `get content`() = runBlocking<Unit> {
        val data = "<html></html>".toByteArray()
        val content = UnionEmbeddedContent(
            id = randomString(),
            mimeType = "text/html",
            available = true,
            size = data.size,
            data = data
        )

        embeddedContentStorage.save(content)

        val result = testRestTemplate.getForEntity("$baseUri/content/embedded/${content.id}", ByteArray::class.java)
        assertThat(result.body).isEqualTo(data)
        assertThat(result.headers.getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo(content.mimeType)
        assertThat(result.headers.getFirst(HttpHeaders.CONTENT_LENGTH)).isEqualTo(content.size.toString())
        assertThat(result.headers.getFirst(HttpHeaders.CONTENT_DISPOSITION)).isEqualTo(
            "attachment; filename=\"${content.id}.html\""
        )
    }
}
