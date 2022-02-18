package com.rarible.protocol.union.enrichment.meta

import com.rarible.loader.cache.CacheEntry
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.time.withTimeoutOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.Closeable
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

@Component
class UnionMetaLoadingAwaitService(
    private val unionMetaMetrics: UnionMetaMetrics
) {
    private val awaitingHandles = ConcurrentHashMap<ItemIdDto, AwaitingHandle>()

    private val logger = LoggerFactory.getLogger(UnionMetaLoadingAwaitService::class.java)

    init {
        unionMetaMetrics.registerMetaLoadingAwaitingItemsGauge { awaitingHandles.size }
    }

    fun onMetaEvent(itemId: ItemIdDto, cacheEntry: CacheEntry<UnionMeta>) {
        awaitingHandles[itemId]?.cacheEntryRef?.set(cacheEntry)
    }

    suspend fun waitForMetaLoadingWithTimeout(
        itemId: ItemIdDto,
        timeout: Duration
    ): UnionMeta? {
        // TODO: if we have 2 instances of union-listener, 1 will miss the event and return null. Think how to synchronize them.
        logger.info("Starting to wait for the meta loading of ${itemId.fullId()} for ${timeout.toMillis()} ms")
        return register(itemId).use { awaitingHandle ->
            withTimeoutOrNull(timeout) {
                while (isActive) {
                    val cacheEntry = awaitingHandle.cacheEntryRef.get()
                    if (cacheEntry != null && cacheEntry.isMetaInitiallyLoadedOrFailed()) {
                        val meta = cacheEntry.getAvailable()
                        if (meta == null) {
                            unionMetaMetrics.onMetaCacheMiss(itemId, timeout)
                        }
                        return@withTimeoutOrNull meta
                    }
                    delay(100)
                }
                return@withTimeoutOrNull null
            }
        }
    }

    private fun register(itemId: ItemIdDto): AwaitingHandle {
        val awaitingHandle = AwaitingHandle(itemId)
        awaitingHandles[itemId] = awaitingHandle
        return awaitingHandle
    }

    private fun deregister(itemId: ItemIdDto) {
        awaitingHandles.remove(itemId)
    }

    private inner class AwaitingHandle(val itemId: ItemIdDto) : Closeable {

        val cacheEntryRef = AtomicReference<CacheEntry<UnionMeta>>()

        override fun close() {
            deregister(itemId)
        }
    }
}
