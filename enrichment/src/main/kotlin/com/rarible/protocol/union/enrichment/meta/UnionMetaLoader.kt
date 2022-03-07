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
            ) {
                val content = enrichContentMetaWithTimeout(unionMeta.content, itemId)
                unionMeta.copy(content = content ?: unionMeta.content)
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
    ): List<UnionMetaContent>? {
        return try {
            withTimeout(Duration.ofMillis(metaProperties.mediaFetchTimeout.toLong())) {
                enrichContentMeta(metaContent, itemId)
            }
        } catch (e: CancellationException) {
            logger.info("Content meta resolution for ${itemId.fullId()}: timeout of ${metaProperties.mediaFetchTimeout.toLong()} ms")
            null
        }
    }

    private suspend fun enrichContentMeta(
        metaContent: List<UnionMetaContent>,
        itemId: ItemIdDto
    ): List<UnionMetaContent> {
        return coroutineScope {
            metaContent.map { content ->
                async {
                    val resolvedUrl = ipfsUrlResolver.resolveRealUrl(content.url)
                    logger.info(
                        "Content meta resolution for ${itemId.fullId()} by URL ${content.url}: " +
                                if (resolvedUrl != content.url) " resolved as $resolvedUrl" else ""
                    )

                    val contentProperties = unionContentMetaLoader.fetchContentMeta(resolvedUrl, itemId)
                    content.copy(url = resolvedUrl, properties = contentProperties)
                }
            }.awaitAll()
        }
    }

    class UnionMetaResolutionException(message: String) : RuntimeException(message)
}
