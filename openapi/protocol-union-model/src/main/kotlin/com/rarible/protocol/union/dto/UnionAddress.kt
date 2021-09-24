package com.rarible.protocol.union.dto

data class UnionAddress(
    override val blockchain: BlockchainDto,
    override val value: String
) : UnionBlockchainId