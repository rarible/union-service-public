package com.rarible.protocol.union.enrichment.meta.content

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.UnionMetrics
import com.rarible.protocol.union.core.model.UnionAudioProperties
import com.rarible.protocol.union.core.model.UnionHtmlProperties
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMetaContentProperties
import com.rarible.protocol.union.core.model.UnionModel3dProperties
import com.rarible.protocol.union.core.model.UnionUnknownProperties
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.dto.BlockchainDto
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class ContentMetaMetrics(
    meterRegistry: MeterRegistry
) : UnionMetrics(meterRegistry) {

    // --------------- Content meta resolution ---------------//
    // Content resolution statistics (for embedded and remote meta content)

    fun onContentFetched(
        blockchain: BlockchainDto,
        start: Instant,
        source: String,
        approach: String,
        properties: UnionMetaContentProperties
    ) {
        val fullness = when {
            properties.isFull() -> "full"
            properties.isEmpty() -> "empty"
            else -> "partial"
        }

        val type = when (properties) {
            is UnionImageProperties -> "image"
            is UnionVideoProperties -> "video"
            is UnionAudioProperties -> "audio"
            is UnionModel3dProperties -> "model"
            is UnionHtmlProperties -> "html"
            is UnionUnknownProperties -> "unknown"
        }

        increment(
            CONTENT_META_RESOLUTION,
            tag(blockchain),
            status("ok"),
            tag("source", source),
            type(type),
            tag("approach", approach),
            tag("fullness", fullness),
            tag("reason", "ok")
        )
    }

    fun onContentResolutionFailed(
        blockchain: BlockchainDto,
        start: Instant,
        source: String,
        approach: String,
        reason: String
    ) {
        record(
            CONTENT_META_RESOLUTION,
            Duration.between(start, nowMillis()),
            tag(blockchain),
            status("fail"),
            tag("source", source),
            type("unknown"),
            tag("approach", approach),
            tag("fullness", "empty"),
            reason(reason)
        )
    }

    // ------------------- Content cache ---------------------//
    // Content cache usage statistics

    fun onContentCacheHit(blockchain: BlockchainDto, cacheType: String) {
        increment(CONTENT_META_CACHE, tag(blockchain), status("hit"), tag("cache_type", cacheType))
    }

    fun onContentCacheMiss(blockchain: BlockchainDto, cacheType: String) {
        increment(CONTENT_META_CACHE, tag(blockchain), status("miss"), tag("cache_type", cacheType))
    }

    // We are not using cache for all types of URL, just for IPFS ATM
    // Content resolution without cache usage will be marked as "skipped"
    fun onContentCacheSkipped(blockchain: BlockchainDto) {
        // TODO cache_type not needed, but prom requires same tags for single metric
        increment(CONTENT_META_CACHE, tag(blockchain), status("skip"), tag("cache_type", "none"))
    }

    // ---------------- Content cache updates ----------------//
    // Metrics to control cache fulfillment

    fun onContentCacheUpdated(blockchain: BlockchainDto, cacheType: String) {
        increment(
            CONTENT_META_CACHE_UPDATE,
            tag(blockchain),
            status("ok"),
            tag("cache_type", cacheType)
        )
    }

    // Even if URL is cacheable, we can skip cache update, if Content is not resolved completely
    fun onContentCacheNotUpdated(blockchain: BlockchainDto, cacheType: String, reason: String) {
        increment(
            CONTENT_META_CACHE_UPDATE,
            tag(blockchain),
            status("fail"),
            tag("cache_type", cacheType),
            reason(reason)
        )
    }

    private companion object {

        const val CONTENT_META_RESOLUTION = "content_meta_resolution"
        const val CONTENT_META_CACHE = "content_meta_cache"
        const val CONTENT_META_CACHE_UPDATE = "content_meta_cache_update"
    }
}
