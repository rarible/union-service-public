package com.rarible.protocol.union.worker.task.search.collection

import com.rarible.protocol.union.dto.BlockchainDto

data class CollectionTaskParam(
    val blockchain: BlockchainDto,
    val index: String
)