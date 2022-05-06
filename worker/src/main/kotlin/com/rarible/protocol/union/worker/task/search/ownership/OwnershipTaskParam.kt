package com.rarible.protocol.union.worker.task.search.ownership

import com.rarible.protocol.union.dto.BlockchainDto

/**
 * Reindexing task state
 *
 * @param blockchain blockchain
 * @param index elasticsearch index name, including environment, version, etc.
 */
data class OwnershipTaskParam(
    val blockchain: BlockchainDto,
    val target: Target,
    val index: String,
) {
    enum class Target {
        OWNERSHIP,
        AUCTIONED_OWNERSHIP,
    }
}
