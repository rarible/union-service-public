package com.rarible.protocol.union.enrichment.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.ContentMetaService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.APP)
class EnrichmentMetaService(
    private val router: BlockchainRouter<ItemService>,
    private val contentMetaService: ContentMetaService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun enrichMeta(itemId: ItemIdDto, meta: UnionMeta): UnionMeta? {
        val enrichedContent = coroutineScope {
            meta.content.map { async { contentMetaService.enrichContent(it, itemId) } }
        }.awaitAll()
        return meta.copy(content = enrichedContent)
    }

    suspend fun resetMeta(itemId: ItemIdDto) {
        // TODO[meta-3.0]: re-implement to not request meta here. Record to database with [itemId] and delete by this key.
        val meta = getItemMeta(itemId)
        meta?.let {
            meta.content.forEach { contentMetaService.resetContentMeta(it.url) }
        }
    }

    suspend fun getItemMeta(itemId: ItemIdDto): UnionMeta? {
        return try {
            router.getService(itemId.blockchain).getItemMetaById(itemId.value)
        } catch (e: WebClientResponseProxyException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                logger.warn("Raw meta for Item [{}] not found", itemId)
                null
            } else {
                throw e
            }
        }
    }
}
