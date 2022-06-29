package com.rarible.protocol.union.enrichment.meta.embedded

import com.rarible.protocol.union.enrichment.configuration.EmbeddedContentProperties
import org.springframework.stereotype.Component

@Component
class EmbeddedContentUrlProvider(
    properties: EmbeddedContentProperties
) {

    val embeddedUrl = properties.publicUrl.trimEnd('/')

    fun isEmbeddedContentUrl(url: String): Boolean {
        return url.startsWith(EMBEDDED_SCHEMA)
    }

    fun getPublicUrl(url: String): String {
        if (isEmbeddedContentUrl(url)) {
            val embeddedId = url.substring(EMBEDDED_SCHEMA.length)
            return getPublicUrlById(embeddedId)
        }
        // Return as is if this is not an embedded URL
        return url
    }

    fun getPublicUrlById(embeddedId: String): String {
        return "$embeddedUrl/$embeddedId"
    }

    fun getSchemaUrl(id: String): String {
        return "$EMBEDDED_SCHEMA$id"
    }

    companion object {

        const val EMBEDDED_SCHEMA = "embedded://"
    }

}