package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.core.exception.UnionMetaException
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.ContentOwner
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.meta.content.ContentMetaDownloader
import com.rarible.protocol.union.enrichment.meta.provider.MetaProvider
import com.rarible.protocol.union.enrichment.util.sanitizeContent
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus

abstract class MetaDownloader<K, T : ContentOwner<T>>(
    private val contentMetaLoader: ContentMetaDownloader,
    private val customizers: List<MetaCustomizer<K, T>>,
    private val providers: List<MetaProvider<K, T>>,
    private val metrics: MetaMetrics,
    val type: String
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    abstract fun generaliseKey(key: K): Pair<String, BlockchainDto>
    abstract suspend fun getRawMeta(key: K): T

    protected suspend fun load(key: K): T? {
        val meta = getMeta(key) ?: providers.firstNotNullOfOrNull { it.fetch(key) }
        meta ?: return null

        val sanitized = sanitizeContent(meta.data.content)
        val (id, blockchain) = generaliseKey(key)
        val content = contentMetaLoader.enrichContent(id, blockchain, sanitized)
        val initial = meta.copy(data = meta.data.withContent(content))
        return customizers.fold(initial) { current, customizer ->
            customizer.customize(key, current)
        }.data
    }

    private suspend fun getMeta(key: K): WrappedMeta<T>? {
        val (id, blockchain) = generaliseKey(key)
        try {
            val result = getRawMeta(key)
            metrics.onMetaFetched(blockchain)
            return WrappedMeta(
                source = MetaSource.ORIGINAL,
                data = result
            )
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

}
