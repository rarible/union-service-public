package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.core.exception.UnionMetaException
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.ContentOwner
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.meta.content.ContentMetaDownloader
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus

abstract class MetaDownloader<K, T : ContentOwner<T>>(
    private val contentMetaLoader: ContentMetaDownloader,
    private val customizers: List<MetaCustomizer<K, T>>,
    private val metrics: MetaMetrics,
    val type: String
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    abstract fun generaliseKey(key: K): Pair<String, BlockchainDto>
    abstract suspend fun getRawMeta(key: K): T

    protected open fun isSimpleHashSupported(key: K): Boolean = false
    protected open suspend fun getSimpleHashMeta(key: K): T? = null

    // By default, we always return raw meta
    protected open fun mergeMeta(raw: T?, simpleHash: T?) = raw

    protected suspend fun load(key: K): T? {
        val meta = fetchMeta(key)
        meta ?: return null

        val sanitized = sanitizeContent(meta.content)
        val (id, blockchain) = generaliseKey(key)
        val content = contentMetaLoader.enrichContent(id, blockchain, sanitized)
        return customizers.fold(meta.withContent(content)) { current, customizer ->
            customizer.customize(key, current)
        }
    }

    private suspend fun fetchMeta(key: K): T? = coroutineScope {
        val raw = async { fetchRawMeta(key) }
        val simpleHash = async {
            if (isSimpleHashSupported(key)) {
                fetchSimpleHashMeta(key)
            } else null
        }
        mergeMeta(raw.await(), simpleHash.await())
    }

    private suspend fun fetchSimpleHashMeta(key: K): T? {
        val (id, blockchain) = generaliseKey(key)
        try {
            val result = getSimpleHashMeta(key)
            metrics.onMetaSimpleHash(blockchain)
            return result
        } catch (e: Exception) {
            logger.error("Meta fetching from simple hash failed for $id",e)
        }
        return null
    }

    private suspend fun fetchRawMeta(key: K): T? {
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
