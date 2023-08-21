package com.rarible.protocol.union.enrichment.meta

import com.rarible.protocol.union.core.model.ContentOwner
import com.rarible.protocol.union.core.model.download.MetaSource
import com.rarible.protocol.union.core.model.download.PartialDownloadException
import com.rarible.protocol.union.core.model.download.ProviderDownloadException
import com.rarible.protocol.union.enrichment.meta.content.MetaContentEnrichmentService
import com.rarible.protocol.union.enrichment.meta.provider.MetaCustomProvider
import com.rarible.protocol.union.enrichment.meta.provider.MetaProvider
import org.slf4j.LoggerFactory
import java.util.concurrent.ArrayBlockingQueue

abstract class MetaDownloader<K, T : ContentOwner<T>>(
    private val metaContentEnrichmentService: MetaContentEnrichmentService<K, T>,
    private val providers: List<MetaProvider<T>>,
    private val customProviders: List<MetaCustomProvider<T>>,
    private val metrics: MetaMetrics,
    val type: String
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    protected suspend fun load(key: K): T? {
        val (id, blockchain) = metaContentEnrichmentService.extractBlockchain(key)

        // Check custom providers first - if any of them can be used for meta resolution,
        // then return result obtained from matched provider (global providers omitted in such case,
        // since there is no sense to use them or trigger "partial retries")
        customProviders.forEach { provider ->
            val result = provider.fetch(blockchain, id)
            if (result.supported) {
                return result.data?.let { metaContentEnrichmentService.enrcih(key = key, meta = it) }
            }
        }

        val failedProviders = ArrayBlockingQueue<MetaSource>(providers.size)
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
