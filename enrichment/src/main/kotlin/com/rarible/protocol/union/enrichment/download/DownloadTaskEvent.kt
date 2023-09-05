package com.rarible.protocol.union.enrichment.download

import java.time.Instant

data class DownloadTaskEvent(
    val id: String,
    val pipeline: String,
    val force: Boolean,
    val source: DownloadTaskSource = DownloadTaskSource.INTERNAL,
    val scheduledAt: Instant,
    val priority: Int = 0
)
