package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.meta.resource.MetaLogger
import com.rarible.core.meta.resource.parser.UrlResourceParsingProcessor
import com.rarible.core.meta.resource.resolver.UrlResolver
import org.springframework.stereotype.Component

// Taken from ethereum-indexer with small modifications
@Component
class UrlService(
    private val urlResourceProcessor: UrlResourceParsingProcessor,
    private val urlResolver: UrlResolver
) {

    // Used only for internal operations, such urls should NOT be stored anywhere
    fun resolveInnerHttpUrl(url: String, id: String) = resolveInternal(url, false, id)

    // Used to build url exposed to the DB cache or API responses
    fun resolvePublicHttpUrl(url: String, id: String) = resolveInternal(url, true, id)

    private fun resolveInternal(url: String, isPublic: Boolean, id: String): String {
        val resource = urlResourceProcessor.parse(url)
        if (resource == null) {
            MetaLogger.logMetaLoading(id = id, message = "UrlService: Cannot parse and resolve url: $url", warn = true)
            return ""
        }
        return if (isPublic) {
            urlResolver.resolvePublicLink(resource)
        } else {
            urlResolver.resolveInnerLink(resource)
        }
    }
}
