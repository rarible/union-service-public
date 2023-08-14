package com.rarible.protocol.union.enrichment.meta

import com.rarible.protocol.union.core.model.download.MetaSource
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.core.UnionMetrics
import io.micrometer.core.instrument.MeterRegistry

open class MetaMetrics(
    meterRegistry: MeterRegistry,
    private val type: String
) : UnionMetrics(meterRegistry) {

    // -------------------- Meta fetch -----------------------//
    // Set of metrics to gather statistics for meta fetching from blockchains

    fun onMetaFetched(blockchain: BlockchainDto, source: MetaSource = MetaSource.ORIGINAL) {
        increment(
            META_FETCH,
            type(type),
            tag(blockchain),
            status("ok"),
            reason("ok"),
            source(source.value)
        ) // TODO should be separate metric
    }

    fun onMetaFetchNotFound(blockchain: BlockchainDto, source: MetaSource = MetaSource.ORIGINAL) {
        increment(
            META_FETCH,
            type(type),
            tag(blockchain),
            status("fail"),
            reason("not_found"),
            source(source.value)
        )
    }

    // TODO not sure it is possible to detect timeouts ATM
    fun onMetaFetchTimeout(blockchain: BlockchainDto, source: MetaSource = MetaSource.ORIGINAL) {
        increment(
            META_FETCH,
            type(type),
            tag(blockchain),
            status("fail"),
            reason("timeout"),
            source(source.value)
        )
    }

    fun onMetaError(blockchain: BlockchainDto, source: MetaSource = MetaSource.ORIGINAL) {
        increment(
            META_FETCH,
            type(type),
            tag(blockchain),
            status("fail"),
            reason("error"),
            source(source.value)
        )
    }

    fun onMetaCorruptedUrlError(blockchain: BlockchainDto, source: MetaSource = MetaSource.ORIGINAL) {
        increment(
            META_FETCH,
            type(type),
            tag(blockchain),
            status("fail"),
            reason("corrupted_url"),
            source(source.value)
        )
    }

    fun onMetaCorruptedDataError(blockchain: BlockchainDto, source: MetaSource = MetaSource.ORIGINAL) {
        increment(
            META_FETCH,
            type(type),
            tag(blockchain),
            status("fail"),
            reason("corrupted_data"),
            source(source.value)
        )
    }

    // --------------------- Meta cache ----------------------//
    // Cached and contains meta
    fun onMetaCacheHit(blockchain: BlockchainDto) {
        increment(META_CACHE, type(type), tag(blockchain), status("hit"))
    }

    // Means cached with status != OK
    fun onMetaCacheEmpty(blockchain: BlockchainDto) {
        increment(META_CACHE, type(type), tag(blockchain), status("empty"))
    }

    // Not found in cache at all
    fun onMetaCacheMiss(blockchain: BlockchainDto) {
        increment(META_CACHE, type(type), tag(blockchain), status("miss"))
    }

    private companion object {

        const val META_FETCH = "meta_fetch"
        const val META_CACHE = "meta_cache"
    }
}
