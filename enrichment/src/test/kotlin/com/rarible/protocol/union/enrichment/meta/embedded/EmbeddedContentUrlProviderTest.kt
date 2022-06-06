package com.rarible.protocol.union.enrichment.meta.embedded

import com.rarible.protocol.union.enrichment.configuration.EmbeddedContentProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EmbeddedContentUrlProviderTest {

    private val publicUrl = "http://localhost:8080/media/"

    private val provider = EmbeddedContentUrlProvider(
        properties = EmbeddedContentProperties(publicUrl)
    )

    @Test
    fun `is embedded url`() {
        assertThat(provider.isEmbeddedContentUrl("embedded://abc")).isTrue()
        assertThat(provider.isEmbeddedContentUrl("http://embedded://abc")).isFalse()
        assertThat(provider.isEmbeddedContentUrl("http://localhost")).isFalse()
    }

    @Test
    fun `get public url`() {
        assertThat(provider.getPublicUrl("embedded://abc")).isEqualTo("http://localhost:8080/media/abc")
        assertThat(provider.getPublicUrl("http://localhost:8080/abc")).isEqualTo("http://localhost:8080/abc")
    }

    @Test
    fun `get schema url`() {
        assertThat(provider.getSchemaUrl("abc")).isEqualTo("embedded://abc")
    }

    @Test
    fun `get public url by id`() {
        assertThat(provider.getPublicUrlById("abc")).isEqualTo("http://localhost:8080/media/abc")
    }

}