package com.rarible.protocol.union.enrichment.meta

import com.rarible.protocol.union.core.model.ContentOwner
import com.rarible.protocol.union.core.model.download.MetaSource
import com.rarible.protocol.union.core.model.download.PartialDownloadException
import com.rarible.protocol.union.core.model.download.ProviderDownloadException
import com.rarible.protocol.union.enrichment.meta.content.MetaContentEnrichmentService
import com.rarible.protocol.union.enrichment.meta.provider.MetaProvider
import org.slf4j.LoggerFactory
import java.util.concurrent.ArrayBlockingQueue

abstract class MetaDownloader<K, T : ContentOwner<T>>(
    private val metaContentEnrichmentService: MetaContentEnrichmentService<K, T>,
    private val providers: List<MetaProvider<T>>,
    private val metrics: MetaMetrics,
    val type: String
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    protected suspend fun load(key: K): T? {
        val failedProviders = ArrayBlockingQueue<MetaSource>(providers.size)
        val (id, blockchain) = metaContentEnrichmentService.extractBlockchain(key)

        val meta = providers.fold(null as T?) { current, provider ->
            try {
                provider.fetch(blockchain, id, current)
            } catch (e: ProviderDownloadException) {
                failedProviders.add(e.provider)
                current
            }
        }
        meta ?: return null

        val result = metaContentEnrichmentService.enrcih(key = key, meta = meta)
        return when {
            failedProviders.isEmpty() -> result
            (failedProviders.size == providers.size) -> null
            else -> throw PartialDownloadException(failedProviders = failedProviders.toList(), data = result)
        }
    }
}
