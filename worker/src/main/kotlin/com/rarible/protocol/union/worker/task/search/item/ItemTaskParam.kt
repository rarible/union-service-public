package com.rarible.protocol.union.worker.task.search.item

import com.rarible.protocol.union.dto.BlockchainDto

data class ItemTaskParam(
    val blockchain: BlockchainDto,
    val index: String
)