package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.core.exception.UnionMetaException
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.ContentOwner
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.meta.content.ContentMetaDownloader
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus

abstract class MetaDownloader<K, T : ContentOwner<T>>(
    private val unionContentMetaLoader: ContentMetaDownloader,
    private val metrics: MetaMetrics,
    val type: String
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    abstract fun generaliseKey(key: K): Pair<String, BlockchainDto>
    abstract suspend fun getRawMeta(key: K): T

    protected suspend fun load(key: K): T? {
        val meta = getMeta(key)
        meta ?: return null

        val sanitized = sanitizeContent(meta.content)
        val (id, blockchain) = generaliseKey(key)
        val content = unionContentMetaLoader.enrichContent(id, blockchain, sanitized)
        return meta.withContent(content)
    }

    private suspend fun getMeta(key: K): T? {
        val (id, blockchain) = generaliseKey(key)
        try {
            val result = getRawMeta(key)
            metrics.onMetaFetched(blockchain)
            return result
        } catch (e: UnionMetaException) {
            logger.error("Meta fetching failed with code: {} for $type {}", e.code.name, id)

            when (e.code) {
                UnionMetaException.ErrorCode.NOT_FOUND -> metrics.onMetaFetchNotFound(blockchain)
                UnionMetaException.ErrorCode.CORRUPTED_URL -> metrics.onMetaCorruptedUrlError(blockchain)
                UnionMetaException.ErrorCode.CORRUPTED_DATA -> metrics.onMetaCorruptedDataError(blockchain)
                UnionMetaException.ErrorCode.TIMEOUT -> metrics.onMetaFetchTimeout(blockchain)
                UnionMetaException.ErrorCode.ERROR -> metrics.onMetaError(blockchain)
            }
            throw e
        } catch (e: UnionNotFoundException) {
            onMetaNotFound(id, blockchain)
        } catch (e: WebClientResponseProxyException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                // this log tagged by itemId, used in Kibana in analytics dashboards
                onMetaNotFound(id, blockchain)
            } else {
                onMetaUnknownError(id, blockchain, e)
            }
        } catch (e: Exception) {
            onMetaUnknownError(id, blockchain, e)
        }
        return null
    }

    private fun onMetaUnknownError(id: String, blockchain: BlockchainDto, exception: Exception) {
        logger.error("Meta fetching failed with code: UNKNOWN for $type {}", id)
        metrics.onMetaError(blockchain)

        throw exception
    }

    private fun onMetaNotFound(id: String, blockchain: BlockchainDto) {
        logger.warn("Meta fetching failed with code: NOT_FOUND for $type {}", id)
        metrics.onMetaFetchNotFound(blockchain)
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
