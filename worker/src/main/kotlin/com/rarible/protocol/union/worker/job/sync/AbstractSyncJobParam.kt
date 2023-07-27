package com.rarible.protocol.union.worker.job.sync

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
