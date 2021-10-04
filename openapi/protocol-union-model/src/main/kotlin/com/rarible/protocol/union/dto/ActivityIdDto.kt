package com.rarible.protocol.union.dto

data class ActivityIdDto(
    override val blockchain: BlockchainDto,
    override val value: String
) : UnionBlockchainId
