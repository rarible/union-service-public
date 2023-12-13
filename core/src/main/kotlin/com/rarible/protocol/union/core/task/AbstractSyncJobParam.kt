package com.rarible.protocol.union.core.task

import com.rarible.protocol.union.dto.BlockchainDto

abstract class AbstractSyncJobParam {
    abstract val blockchain: BlockchainDto
    abstract val scope: SyncScope
    abstract val esIndex: String?
    abstract val batchSize: Int
    abstract val chunkSize: Int

    companion object {
        const val DEFAULT_CHUNK = 20
        const val DEFAULT_BATCH = 200
    }
}

enum class SyncScope {
    // Only save to union DB, ES data won't be updated, no events
    DB,

    // Update data in DB and ES without events
    ES,

    // Update data in DB and send events (ES data will be updated via these events)
    EVENT
}
