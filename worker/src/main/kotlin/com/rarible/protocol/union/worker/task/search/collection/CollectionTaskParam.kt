package com.rarible.protocol.union.worker.task.search.collection

import com.rarible.protocol.union.dto.BlockchainDto

class CollectionTaskParam(
    val blockchain: BlockchainDto,
    val index: String
)