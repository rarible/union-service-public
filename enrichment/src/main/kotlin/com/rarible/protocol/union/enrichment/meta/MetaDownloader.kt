package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.ContentOwner
import com.rarible.protocol.union.core.model.MetaSource
import com.rarible.protocol.union.enrichment.download.PartialDownloadException
import com.rarible.protocol.union.enrichment.download.ProviderDownloadException
import com.rarible.protocol.union.enrichment.meta.content.MetaContentEnrichmentService
import com.rarible.protocol.union.enrichment.meta.provider.MetaCustomProvider
import com.rarible.protocol.union.enrichment.meta.provider.MetaProvider
import com.rarible.protocol.union.enrichment.util.metaSpent
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
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
        val start = nowMillis()
        val (id, blockchain) = metaContentEnrichmentService.extractBlockchain(key)

        // Check custom providers first - if any of them can be used for meta resolution,
        // then return result obtained from matched provider (global providers omitted in such case,
        // since there is no sense to use them or trigger "partial retries")
        customProviders.forEach { provider ->
            val result = provider.fetch(blockchain, id)
            if (result.supported) {
                val enrichmentStart = nowMillis()
                val enriched = result.data?.let { metaContentEnrichmentService.enrich(key = key, meta = it) }
                logDownload(start, enrichmentStart, key, enriched)
                return enriched
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

        if (meta == null) {
            logDownload(start, nowMillis(), key, null)
            return null
        }

        val enrichmentStart = nowMillis()
        val result = metaContentEnrichmentService.enrich(key = key, meta = meta)
        logDownload(start, enrichmentStart, key, result)

        return when {
            failedProviders.isEmpty() -> result
            (failedProviders.size == providers.size) -> null
            else -> throw PartialDownloadException(failedProviders = failedProviders.toList(), data = result)
        }
    }

    private fun logDownload(
        start: Instant,
        enrichmentStart: Instant,
        key: K,
        meta: T?
    ) {
        val jsonDuration = Duration.between(start, enrichmentStart)
        if (meta != null) {
            logger.info(
                "Downloaded $type meta for $key" +
                    " (${metaSpent(jsonDuration)} json, ${metaSpent(enrichmentStart)}) enrichment"
            )
        } else {
            logger.info(
                "Failed to download $type meta for $key" +
                    " (${metaSpent(jsonDuration)} json, ${metaSpent(enrichmentStart)}) enrichment"
            )
        }
    }
}
