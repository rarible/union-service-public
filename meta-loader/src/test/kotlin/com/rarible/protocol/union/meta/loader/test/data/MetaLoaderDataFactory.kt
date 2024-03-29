package com.rarible.protocol.union.meta.loader.test.data

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.enrichment.download.DownloadEntry
import com.rarible.protocol.union.enrichment.download.DownloadStatus
import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import com.rarible.protocol.union.enrichment.download.DownloadTaskSource
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta

fun randomMetaEntry(itemId: String, meta: UnionMeta = randomUnionMeta()): DownloadEntry<UnionMeta> {
    val now = nowMillis()
    return DownloadEntry(
        id = itemId,
        status = DownloadStatus.SUCCESS,
        downloads = 1,
        data = meta,
        scheduledAt = now.minusSeconds(1),
        updatedAt = now,
        succeedAt = now
    )
}

fun randomRetryMetaEntry(itemId: String): DownloadEntry<UnionMeta> {
    val now = nowMillis()
    return DownloadEntry(
        id = itemId,
        status = DownloadStatus.RETRY,
        scheduledAt = now.minusSeconds(1),
        updatedAt = now,
        failedAt = now,
        retries = 1,
        fails = 1
    )
}

fun randomFailedMetaEntry(itemId: String): DownloadEntry<UnionMeta> {
    val now = nowMillis()
    return DownloadEntry(
        id = itemId,
        status = DownloadStatus.FAILED,
        scheduledAt = now.minusSeconds(1),
        updatedAt = now,
        failedAt = now,
        retries = 5,
        fails = 5
    )
}

fun randomTaskEvent(
    itemId: String,
    force: Boolean = false
): DownloadTaskEvent {
    return DownloadTaskEvent(
        id = itemId,
        pipeline = "test",
        force = force,
        scheduledAt = nowMillis(),
        source = DownloadTaskSource.INTERNAL
    )
}
