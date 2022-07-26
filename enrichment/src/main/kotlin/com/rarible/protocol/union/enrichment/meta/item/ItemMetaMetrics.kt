package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.metrics.UnionMetrics
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class ItemMetaMetrics(
    meterRegistry: MeterRegistry
) : UnionMetrics(meterRegistry) {

    //-------------------- Meta fetch -----------------------//
    // Set of metrics to gather statistics for meta fetching from blockchains

    fun onMetaFetched(blockchain: BlockchainDto) {
        increment(META_FETCH, tag(blockchain), status("ok"), reason("ok")) // TODO should be separate metric
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

    private companion object {

        const val META_FETCH = "meta_fetch"
        const val META_CACHE = "meta_cache"
    }

}
