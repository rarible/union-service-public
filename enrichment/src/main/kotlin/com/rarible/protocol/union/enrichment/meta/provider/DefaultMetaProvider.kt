package com.rarible.protocol.union.enrichment.meta.provider

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.core.exception.UnionMetaException
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.ContentOwner
import com.rarible.protocol.union.core.model.MetaSource
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.download.ProviderDownloadException
import com.rarible.protocol.union.enrichment.meta.MetaMetrics
import io.netty.handler.timeout.ReadTimeoutException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus

abstract class DefaultMetaProvider<T : ContentOwner<T>>(
    private val metrics: MetaMetrics,
    val type: String
) : MetaProvider<T> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getSource() = MetaSource.ORIGINAL

    abstract suspend fun fetch(blockchain: BlockchainDto, key: String): T

    override suspend fun fetch(blockchain: BlockchainDto, id: String, original: T?): T? {
        try {
            val result = fetch(blockchain, id)
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
        } catch (e: UnionNotFoundException) {
            onMetaNotFound(id, blockchain)
        } catch (e: WebClientResponseProxyException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                // this log tagged by itemId, used in Kibana in analytics dashboards
                onMetaNotFound(id, blockchain)
            } else {
                onMetaUnknownError(id, blockchain, e)
            }
        } catch (e: ReadTimeoutException) {
            onMetaTimeout(id, blockchain, e)
        } catch (e: Exception) {
            onMetaUnknownError(id, blockchain, e)
        }
        throw ProviderDownloadException(MetaSource.ORIGINAL)
    }

    private fun onMetaUnknownError(id: String, blockchain: BlockchainDto, exception: Exception) {
        logger.error("Meta fetching failed with code: UNKNOWN for {} {}", type, id)
        metrics.onMetaError(blockchain)

        throw exception
    }

    private fun onMetaNotFound(id: String, blockchain: BlockchainDto) {
        logger.warn("Meta fetching failed with code: NOT_FOUND for {} {}", type, id)
        metrics.onMetaFetchNotFound(blockchain)
    }

    private fun onMetaTimeout(id: String, blockchain: BlockchainDto, e: Exception) {
        logger.warn("Meta fetching failed with code: TIMEOUT for {} {}: {}", type, id, e.message)
        metrics.onMetaFetchTimeout(blockchain)
    }
}
