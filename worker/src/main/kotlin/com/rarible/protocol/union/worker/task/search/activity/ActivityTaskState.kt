package com.rarible.protocol.union.worker.task.search.activity

import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto

/**
 * Reindexing task state
 *
 * @param blockchain blockchain
 * @param activityType activity type
 * @param index elasticsearch index name, including environment, version, etc.
 * @param cursor next request cursor
 */
data class ActivityTaskState(
    val blockchain: BlockchainDto,
    val activityType: ActivityTypeDto,
    val index: String? = null,
    val cursor: String? = null
) {
    fun next(cursor: String?): ActivityTaskState {
        return copy(cursor = cursor)
    }
}