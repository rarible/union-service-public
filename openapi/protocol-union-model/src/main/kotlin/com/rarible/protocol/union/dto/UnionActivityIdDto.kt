package com.rarible.protocol.union.dto

data class UnionActivityIdDto(
    override val blockchain: BlockchainDto,
    override val value: String
) : UnionBlockchainId
