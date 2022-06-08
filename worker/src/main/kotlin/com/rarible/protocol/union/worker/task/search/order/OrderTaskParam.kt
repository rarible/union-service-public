package com.rarible.protocol.union.worker.task.search.order

import com.rarible.protocol.union.dto.BlockchainDto

data class OrderTaskParam(
    val blockchain: BlockchainDto,
    val index: String
)