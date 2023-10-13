package com.rarible.protocol.union.core.service.router

import com.rarible.protocol.union.dto.BlockchainDto

data class ActiveBlockchain(
    val active: List<BlockchainDto>
)
