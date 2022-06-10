package com.rarible.protocol.union.worker.task.search.collection

import com.rarible.protocol.union.dto.BlockchainDto


data class ChangeEsCollectionAliasTaskParam(
    val indexName: String,
    val blockchains: List<BlockchainDto>
)