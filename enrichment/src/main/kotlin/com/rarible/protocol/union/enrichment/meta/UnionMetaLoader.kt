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
import com.rarible.protocol.union.enrichment.configuration.UnionMetaProperties
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.time.withTimeout
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class UnionMetaLoader(
    private val router: BlockchainRouter<ItemService>,
    private val unionContentMetaLoader: UnionContentMetaLoader,
    private val metaProperties: UnionMetaProperties,
    private val ipfsUrlResolver: IpfsUrlResolver
) {

    private val logger = LoggerFactory.getLogger(UnionMetaLoader::class.java)

    suspend fun load(itemId: ItemIdDto): UnionMeta =
        withTransaction("UnionMetaLoader") {
            val unionMeta = withSpan(
                name = "getItemMetaById",
                type = SpanType.EXT,
                labels = listOf("itemId" to itemId.fullId())
            ) {
                getItemMeta(itemId) ?: throw UnionMetaResolutionException("Cannot resolve meta for ${itemId.fullId()}")
            }
            withSpan(
                name = "enrichContentMeta",
                labels = listOf("itemId" to itemId.fullId())
            ) { enrichContentMeta(unionMeta, itemId) }
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

    private suspend fun enrichContentMeta(meta: UnionMeta, itemId: ItemIdDto): UnionMeta {
        return meta.copy(content = coroutineScope {
            meta.content.map { async { enrichContentMeta(it, itemId) } }.awaitAll()
        })
    }

    private suspend fun enrichContentMeta(
        content: UnionMetaContent,
        itemId: ItemIdDto
    ): UnionMetaContent {
        val resolvedUrl = ipfsUrlResolver.resolveRealUrl(content.url)
        logger.info(
            "Resolving content meta for item ${itemId.fullId()} for URL ${content.url}" +
                    if (resolvedUrl != content.url) " resolved as $resolvedUrl" else "",
        )
        val enrichedProperties = try {
            withTimeout(Duration.ofMillis(metaProperties.mediaFetchTimeout.toLong())) {
                unionContentMetaLoader.fetchContentMeta(resolvedUrl, itemId)
            }
        } catch (e: CancellationException) {
            logger.info(
                "Timeout of ${metaProperties.mediaFetchTimeout} ms to resolve content meta for ${itemId.fullId()} for URL ${content.url}",
                e
            )
            null
        } catch (e: Exception) {
            logger.info(
                "Failed to resolve content meta for ${itemId.fullId()} for URL ${content.url}",
            )
            null
        }
        return content.copy(url = resolvedUrl, properties = enrichedProperties)
    }

    class UnionMetaResolutionException(message: String) : RuntimeException(message)
}
