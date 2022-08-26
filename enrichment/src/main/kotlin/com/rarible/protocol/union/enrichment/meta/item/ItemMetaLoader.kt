package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.core.apm.SpanType
import com.rarible.core.apm.withSpan
import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.core.exception.UnionMetaException
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.util.LogUtils
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.content.ContentMetaDownloader
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

// TODO refactor with ItemMetaDownloader
@Component
class ItemMetaLoader(
    private val router: BlockchainRouter<ItemService>,
    private val unionContentMetaLoader: ContentMetaDownloader,
    private val metrics: ItemMetaMetrics
) {

    private val logger = LoggerFactory.getLogger(ItemMetaLoader::class.java)

    suspend fun load(itemId: ItemIdDto): UnionMeta? = LogUtils.addToMdc(itemId, router) {
        val unionMeta = withSpan(
            name = "getItemMetaById",
            type = SpanType.EXT,
            labels = listOf("itemId" to itemId.fullId())
        ) {
            getItemMeta(itemId)
        }

        unionMeta ?: return@addToMdc null


        withSpan(
            name = "enrichContentMeta",
            labels = listOf("itemId" to itemId.fullId())
        ) {
            val sanitized = sanitizeContent(unionMeta.content)
            val content = unionContentMetaLoader.enrichContent(itemId, sanitized)
            unionMeta.copy(content = content)
        }
    }

    private suspend fun getItemMeta(itemId: ItemIdDto): UnionMeta? {
        return try {
            val result = router.getService(itemId.blockchain).getItemMetaById(itemId.value)
            metrics.onMetaFetched(itemId.blockchain)
            result
        } catch (e: UnionMetaException) {
            when (e.code) {
                UnionMetaException.ErrorCode.UNPARSEABLE_JSON -> metrics.onMetaParseJsonError(itemId.blockchain)
                UnionMetaException.ErrorCode.UNPARSEABLE_LINK -> metrics.onMetaParseLinkError(itemId.blockchain)
                UnionMetaException.ErrorCode.TIMEOUT -> metrics.onMetaFetchTimeout(itemId.blockchain)
                UnionMetaException.ErrorCode.UNKNOWN -> metrics.onMetaFetchError(itemId.blockchain)
            }
            throw e
        } catch (e: WebClientResponseProxyException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                // this log tagged by itemId, used in Kibana in analytics dashboards
                logger.warn("Meta not found in blockchain for Item {}", itemId)
                metrics.onMetaFetchNotFound(itemId.blockchain)
                null
            } else {
                metrics.onMetaFetchError(itemId.blockchain)
                throw e
            }
        } catch (e: Exception) {
            metrics.onMetaFetchError(itemId.blockchain)
            throw e
        }
    }

    private fun sanitizeContent(content: List<UnionMetaContent>): List<UnionMetaContent> {
        return content.mapNotNull {
            if (it.url.isBlank()) {
                null
            } else {
                it.copy(url = it.url.trim())
            }
        }
    }

}
