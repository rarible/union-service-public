package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.apm.SpanType
import com.rarible.core.apm.withSpan
import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.util.LogUtils
import com.rarible.protocol.union.dto.ItemIdDto
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class UnionMetaLoader(
    private val router: BlockchainRouter<ItemService>,
    private val unionContentMetaLoader: UnionContentMetaLoader
) {

    private val logger = LoggerFactory.getLogger(UnionMetaLoader::class.java)

    suspend fun load(itemId: ItemIdDto): UnionMeta? = LogUtils.addToMdc(itemId, router) {
        val unionMeta = withSpan(
            name = "getItemMetaById",
            type = SpanType.EXT,
            labels = listOf("itemId" to itemId.fullId())
        ) {
            getItemMeta(itemId)
        }

        if (unionMeta == null) {
            // this log tagged by itemId, used in Kibana in analytics dashboards
            logger.warn("Meta not found in blockchain for Item {}", itemId)
            return@addToMdc null
        }

        withSpan(
            name = "enrichContentMeta",
            labels = listOf("itemId" to itemId.fullId())
        ) {
            val content = unionContentMetaLoader.enrichContentMeta(unionMeta.content)
            unionMeta.copy(content = content)
        }
    }

    private suspend fun getItemMeta(itemId: ItemIdDto): UnionMeta? {
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
