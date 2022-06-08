package com.rarible.protocol.union.enrichment.meta

import com.rarible.protocol.union.core.model.UnionAudioProperties
import com.rarible.protocol.union.core.model.UnionHtmlProperties
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMetaContentProperties
import com.rarible.protocol.union.core.model.UnionModel3dProperties
import com.rarible.protocol.union.core.model.UnionUnknownProperties
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.dto.BlockchainDto
import io.micrometer.core.instrument.ImmutableTag
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.springframework.stereotype.Component

@Component
class UnionMetaMetrics(
    private val meterRegistry: MeterRegistry
) {

    //-------------------- Meta fetch -----------------------//
    // Set of metrics to gather statistics for meta fetching from blockchains

    fun onMetaFetched(blockchain: BlockchainDto) {
        increment(META_FETCH, tag(blockchain), status("ok"))
    }

    fun onMetaFetchNotFound(blockchain: BlockchainDto) {
        increment(META_FETCH, tag(blockchain), status("fail"), reason("not_found"))
    }

    // TODO not sure it is possible to detect timeouts ATM
    fun onMetaFetchTimeout(blockchain: BlockchainDto) {
        increment(META_FETCH, tag(blockchain), status("fail"), reason("timeout"))
    }

    fun onMetaFetchError(blockchain: BlockchainDto) {
        increment(META_FETCH, tag(blockchain), status("fail"), reason("error"))
    }

    //--------------------- Meta cache ----------------------//
    // Meta cache usage statistics

    fun onMetaCacheHit(blockchain: BlockchainDto) {
        increment(META_CACHE, tag(blockchain), status("hit"))
    }

    fun onMetaCacheMiss(blockchain: BlockchainDto) {
        increment(META_CACHE, tag(blockchain), status("miss"))
    }

    //--------------- Content meta resolution ---------------//
    // Content resolution statistics (for embedded and remote meta content)

    fun onContentFetched(blockchain: BlockchainDto, source: String, properties: UnionMetaContentProperties) {
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
            tag("fullness", fullness)
        )
    }

    fun onContentResolutionFailed(blockchain: BlockchainDto, source: String, reason: String) {
        increment(
            CONTENT_META_RESOLUTION,
            tag(blockchain),
            status("fail"),
            tag("source", source),
            reason(reason)
        )
    }

    //------------------- Content cache ---------------------//
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
        increment(CONTENT_META_CACHE, tag(blockchain), status("skip"))
    }

    //---------------- Content cache updates ----------------//
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

    private fun tag(blockchain: BlockchainDto): Tag {
        return tag("blockchain", blockchain.name.lowercase())
    }

    private fun tag(key: String, value: String): Tag {
        return ImmutableTag(key, value)
    }

    private fun status(status: String): Tag {
        return tag("status", status)
    }

    private fun type(type: String): Tag {
        return tag("type", type)
    }

    private fun reason(reason: String): Tag {
        return tag("reason", reason)
    }

    private fun increment(name: String, vararg tags: Tag) {
        return meterRegistry.counter(name, tags.toList()).increment()
    }

    private companion object {

        const val META_FETCH = "meta_fetch"
        const val META_CACHE = "meta_cache"
        const val CONTENT_META_RESOLUTION = "content_meta_resolution"
        const val CONTENT_META_CACHE = "content_meta_cache"
        const val CONTENT_META_CACHE_UPDATE = "content_meta_cache_update"
    }

}
