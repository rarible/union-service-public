package com.rarible.protocol.union.core.task

import com.rarible.protocol.union.dto.BlockchainDto

data class RefreshCollectionMetaTaskParam(
    val blockchain: BlockchainDto? = null,
    val full: Boolean = false,
    val priority: Int = 0,
) {
    companion object {
        const val COLLECTION_META_REFRESH_TASK = "COLLECTION_META_REFRESH_TASK"
    }
}
