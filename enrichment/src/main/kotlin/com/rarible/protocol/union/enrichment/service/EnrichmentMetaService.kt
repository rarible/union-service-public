package com.rarible.protocol.union.enrichment.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.configuration.MetaProperties
import com.rarible.protocol.union.enrichment.meta.ContentMetaService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@CaptureSpan(type = SpanType.APP)
class EnrichmentMetaService(
    private val router: BlockchainRouter<ItemService>,
    private val metaProperties: MetaProperties,
    private val contentMetaService: ContentMetaService
) {

    suspend fun enrichMetaWithContentMeta(meta: UnionMeta): UnionMeta {
        val enrichedContent = coroutineScope {
            meta.content.map { async { enrichWithContentMeta(it) } }.awaitAll()
        }
        return meta.copy(content = enrichedContent)
    }

    private suspend fun enrichWithContentMeta(content: UnionMetaContent): UnionMetaContent {
        val properties = content.properties
        if (properties != null && !properties.isEmpty()) {
            return content
        }
        val fetchedProperties = contentMetaService.fetchContentMeta(
            url = content.url,
            timeout = Duration.ofMillis(metaProperties.timeoutLoadingContentMeta)
        )
        val enrichedProperties = fetchedProperties
            ?: properties // Use at least some fields of known properties.
            ?: UnionImageProperties() // Questionable, but let's consider that was an image.
        return content.copy(properties = enrichedProperties)
    }

    suspend fun refreshContentMeta(meta: UnionMeta) {
        coroutineScope {
            meta.content.map { async { contentMetaService.refreshContentMeta(it.url) } }.awaitAll()
        }
    }

    suspend fun getItemMeta(itemId: ItemIdDto): UnionMeta? {
        return try {
            router.getService(itemId.blockchain).getItemMetaById(itemId.value)
        } catch (e: WebClientResponseProxyException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                null
            } else {
                throw e
            }
        }
    }
}
