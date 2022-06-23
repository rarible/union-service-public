package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.loader.LoadTaskStatus
import com.rarible.loader.cache.CacheEntry
import com.rarible.protocol.union.core.model.UnionMeta

/**
 * Returns true if loading of the meta for an item has been scheduled in the past,
 * no matter what the loading result is (in progress, failed or success).
 */
@Deprecated("Should be replaced in epic Meta 3.0: Pipeline")
fun CacheEntry<UnionMeta>.isMetaInitiallyScheduledForLoading(): Boolean =
    when (this) {
        // Let's use full 'when' expression to not forget some if branches.
        is CacheEntry.NotAvailable -> false
        is CacheEntry.Loaded,
        is CacheEntry.LoadedAndUpdateScheduled,
        is CacheEntry.LoadedAndUpdateFailed,
        is CacheEntry.InitialLoadScheduled,
        is CacheEntry.InitialFailed -> true
    }


/**
 * Returns `true` if the meta for item has been loaded or loading has failed,
 * and `false` if we haven't requested the meta loading or haven't received any result yet.
 */
@Deprecated("Should be replaced in epic Meta 3.0: Pipeline")
fun CacheEntry<UnionMeta>.isMetaInitiallyLoadedOrFailed(): Boolean =
    when (this) {
        // Let's use full 'when' expression to not forget some if branches.
        is CacheEntry.Loaded -> true
        is CacheEntry.LoadedAndUpdateScheduled -> true
        is CacheEntry.LoadedAndUpdateFailed -> true
        is CacheEntry.InitialLoadScheduled -> when (loadStatus) {
            is LoadTaskStatus.Scheduled -> false
            is LoadTaskStatus.WaitsForRetry -> true
        }
        is CacheEntry.InitialFailed -> true
        is CacheEntry.NotAvailable -> false
    }

/**
 * Returns meta from this cache entry.
 * If the meta update has failed, the available meta is returned.
 */
fun CacheEntry<UnionMeta>.getAvailable(): UnionMeta? =
    when (this) {
        // Let's use full 'when' expression to not forget some if branches.
        is CacheEntry.Loaded -> data
        is CacheEntry.LoadedAndUpdateScheduled -> data
        is CacheEntry.LoadedAndUpdateFailed -> data
        is CacheEntry.NotAvailable -> null
        is CacheEntry.InitialLoadScheduled -> null
        is CacheEntry.InitialFailed -> null
    }
