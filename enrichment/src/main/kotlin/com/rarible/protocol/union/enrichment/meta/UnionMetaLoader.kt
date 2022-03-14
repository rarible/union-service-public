package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.apm.SpanType
import com.rarible.core.apm.withSpan
import com.rarible.core.apm.withTransaction
import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ItemIdDto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class UnionMetaLoader(
    private val router: BlockchainRouter<ItemService>,
    private val unionContentMetaLoader: UnionContentMetaLoader,
    private val ipfsUrlResolver: IpfsUrlResolver
) {

    private val logger = LoggerFactory.getLogger(UnionMetaLoader::class.java)

    suspend fun load(itemId: ItemIdDto): UnionMeta? =
        withTransaction("UnionMetaLoader") {
            val unionMeta = withSpan(
                name = "getItemMetaById",
                type = SpanType.EXT,
                labels = listOf("itemId" to itemId.fullId())
            ) {
                getItemMeta(itemId)
            } ?: return@withTransaction null
            withSpan(
                name = "enrichContentMeta",
                labels = listOf("itemId" to itemId.fullId())
            ) {
                val content = enrichContentMetaWithTimeout(unionMeta.content, itemId)
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

    private suspend fun enrichContentMetaWithTimeout(
        metaContent: List<UnionMetaContent>,
        itemId: ItemIdDto
    ): List<UnionMetaContent> = coroutineScope {
        metaContent.map { content ->
            val resolvedUrl = ipfsUrlResolver.resolveRealUrl(content.url)
            logger.info(
                "Content meta resolution for ${itemId.fullId()}: content URL ${content.url} was resolved to $resolvedUrl"
            )
            async {
                val contentProperties = unionContentMetaLoader.fetchContentMeta(content.url, itemId)
                content.copy(url = resolvedUrl, properties = contentProperties)
            }
        }.awaitAll()
    }
}
