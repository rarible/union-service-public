package com.rarible.protocol.union.worker.task.search.activity

import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto

/**
 * Reindexing task state
 *
 * @param blockchain blockchain
 * @param type activity type
 * @param index elasticsearch index name, including environment, version, etc.
 */
data class ActivityTaskParam(
    val blockchain: BlockchainDto,
    val type: ActivityTypeDto,
    val index: String
)