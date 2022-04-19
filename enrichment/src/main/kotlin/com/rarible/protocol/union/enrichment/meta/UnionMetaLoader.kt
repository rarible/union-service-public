package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.apm.SpanType
import com.rarible.core.apm.withSpan
import com.rarible.core.apm.withTransaction
import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.core.model.UnionImageProperties
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
            async {
                val resolvedUrl = ipfsUrlResolver.resolveInnerUrl(content.url)
                val publicUrl = ipfsUrlResolver.resolvePublicUrl(content.url)
                val logPrefix = "Content meta resolution for ${itemId.fullId()}"
                logger.info(
                    logPrefix + if (resolvedUrl != content.url)
                        ": content URL ${content.url} was resolved to $resolvedUrl" else ""
                )
                val resolvedContentMeta = unionContentMetaLoader.fetchContentMeta(resolvedUrl, itemId)
                val contentProperties = when {
                    resolvedContentMeta != null -> {
                        logger.info("$logPrefix: resolved to $resolvedContentMeta")
                        resolvedContentMeta
                    }
                    content.properties != null -> {
                        logger.info("$logPrefix: falling back to blockchain's meta ${content.properties}")
                        content.properties
                    }
                    else -> {
                        logger.warn("$logPrefix: falling back to image properties")
                        UnionImageProperties()
                    }
                }
                content.copy(url = publicUrl, properties = contentProperties)
            }
        }.awaitAll()
    }
}
