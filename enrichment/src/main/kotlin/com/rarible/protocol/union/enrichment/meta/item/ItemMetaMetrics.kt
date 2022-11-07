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
        increment(META_FETCH, tag(blockchain), status("fail"), reason("unknown_error"))
    }

    fun onMetaParseLinkError(blockchain: BlockchainDto) {
        increment(META_FETCH, tag(blockchain), status("fail"), reason("meta_parse_link"))
    }

    fun onMetaParseJsonError(blockchain: BlockchainDto) {
        increment(META_FETCH, tag(blockchain), status("fail"), reason("meta_parse_json"))
    }

    //--------------------- Meta cache ----------------------//
    // Cached and contains meta
    fun onMetaCacheHit(blockchain: BlockchainDto) {
        increment(META_CACHE, tag(blockchain), status("hit"))
    }

    // Means cached with status != OK
    fun onMetaCacheEmpty(blockchain: BlockchainDto) {
        increment(META_CACHE, tag(blockchain), status("empty"))
    }

    // Not found in cache at all
    fun onMetaCacheMiss(blockchain: BlockchainDto) {
        increment(META_CACHE, tag(blockchain), status("miss"))
    }

    private companion object {

        const val META_FETCH = "meta_fetch"
        const val META_CACHE = "meta_cache"
    }

}
