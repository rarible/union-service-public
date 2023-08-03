package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.core.exception.UnionMetaException
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.ContentOwner
import com.rarible.protocol.union.core.model.download.MetaProviderType
import com.rarible.protocol.union.core.model.download.PartialDownloadException
import com.rarible.protocol.union.core.model.download.ProviderDownloadException
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.meta.content.MetaContentEnrichmentService
import com.rarible.protocol.union.enrichment.meta.provider.MetaProvider
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import java.util.concurrent.ArrayBlockingQueue
import kotlin.math.max

abstract class MetaDownloader<K, T : ContentOwner<T>>(
    private val metaContentEnrichmentService: MetaContentEnrichmentService<K, T>,
    private val providers: List<MetaProvider<K, T>>,
    private val metrics: MetaMetrics,
    val type: String
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    abstract suspend fun getRawMeta(key: K): T

    protected suspend fun load(key: K): T? {
        val failedProviders = ArrayBlockingQueue<MetaProviderType>(max(providers.size, 1))
        val meta = providers.fold(getMeta(key)) { current, provider ->
            try {
                provider.fetch(key, current)
            } catch (e: ProviderDownloadException) {
                failedProviders.add(e.provider)
                current
            }
        }
        meta ?: return null

        val result = metaContentEnrichmentService.enrcih(key = key, meta = meta)
        return if (failedProviders.isEmpty()) {
            result
        } else {
            throw PartialDownloadException(failedProviders = failedProviders.toList(), data = result)
        }
    }

    private suspend fun getMeta(key: K): T? {
        val (id, blockchain) = metaContentEnrichmentService.generaliseKey(key)
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
}
