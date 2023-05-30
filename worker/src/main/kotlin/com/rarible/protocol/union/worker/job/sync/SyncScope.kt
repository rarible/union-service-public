package com.rarible.protocol.union.worker.job.sync

enum class SyncScope {
    // Only save to union DB, ES data won't be updated, no events
    DB,

    // Update data in DB and ES without events
    ES,

    // Update data in DB and send events (ES data will be updated via these events)
    EVENT
}