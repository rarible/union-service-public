package com.rarible.protocol.union.enrichment.meta.simplehash

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.metrics.UnionMetrics
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class SimpleHashItemMetrics(
    meterRegistry: MeterRegistry
) : UnionMetrics(meterRegistry) {

    fun onEventIncomingSuccess(blockchain: BlockchainDto) {
        increment(SIMPLEHASH_EVENT_INCOMING, type(TYPE), tag(blockchain), status("success"))
    }
    fun onEventIncomingFailed(blockchain: BlockchainDto) {
        increment(SIMPLEHASH_EVENT_INCOMING, type(TYPE), tag(blockchain), status("failed"))
    }
    fun onEventIncomingFailed() {
        increment(SIMPLEHASH_EVENT_INCOMING, type(TYPE), status("failed"))
    }

    fun onMetaCacheChanged(blockchain: BlockchainDto) {
        increment(SIMPLEHASH_META_CACHE, type(TYPE), tag(blockchain), status("changed"))
    }
    fun onMetaCacheSaved(blockchain: BlockchainDto) {
        increment(SIMPLEHASH_META_CACHE, type(TYPE), tag(blockchain), status("saved"))
    }
    fun onMetaCacheRefresh(blockchain: BlockchainDto) {
        increment(SIMPLEHASH_META_CACHE, type(TYPE), tag(blockchain), status("refresh"))
    }

    private companion object {
        const val TYPE: String = "item"
        const val SIMPLEHASH_EVENT_INCOMING = "simplehash_event_incoming"
        const val SIMPLEHASH_META_CACHE = "simplehash_meta_cache"
    }
}
