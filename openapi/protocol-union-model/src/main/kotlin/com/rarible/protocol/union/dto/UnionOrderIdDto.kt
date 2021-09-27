package com.rarible.protocol.union.dto

data class UnionOrderIdDto(
    override val blockchain: BlockchainDto,
    override val value: String
) : UnionBlockchainId