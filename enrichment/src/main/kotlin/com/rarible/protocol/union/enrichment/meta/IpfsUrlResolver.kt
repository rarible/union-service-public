package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.meta.resource.detector.embedded.EmbeddedContentDetectProcessor
import com.rarible.core.meta.resource.parser.UrlResourceParsingProcessor
import com.rarible.core.meta.resource.resolver.UrlResolver
import org.springframework.stereotype.Component

// Taken from ethereum-indexer with small modifications
@Component
class IpfsUrlResolver(                                        // TODO Maybe rename to UrlService
    private val urlResourceProcessor: UrlResourceParsingProcessor,
    private val urlResolver: UrlResolver,
    private val embeddedContentDetectProcessor: EmbeddedContentDetectProcessor
) {

    // Used only for internal operations, such urls should NOT be stored anywhere
    fun resolveInnerHttpUrl(url: String) = resolveInner(url, false)

    // Used to build url exposed to the DB cache or API responses
    fun resolvePublicHttpUrl(url: String) = resolveInner(url, true)

    private fun resolveInner(url: String, isPublic: Boolean): String {
        val embeddedContent = embeddedContentDetectProcessor.decode(url)
        if (embeddedContent != null) return embeddedContent.content.decodeToString()

        val resource = urlResourceProcessor.parse(url) ?: return ""  // TODO Add logging here and maybe throw Exception
        return if (isPublic) {
            urlResolver.resolvePublicLink(resource)
        } else {
            urlResolver.resolveInnerLink(resource)
        }
    }
}
