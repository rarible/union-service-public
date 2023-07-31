package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.BlockchainDto

data class UnionDomainResolveResult(
    val blockchain: BlockchainDto,
    val registrant: String
)
