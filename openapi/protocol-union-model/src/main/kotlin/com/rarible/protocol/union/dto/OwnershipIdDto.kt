package com.rarible.protocol.union.dto

import java.math.BigInteger

data class OwnershipIdDto(
    override val blockchain: BlockchainDto,
    val token: UnionAddress,
    val tokenId: BigInteger,
    val owner: UnionAddress
) : UnionBlockchainId {

    override val value = "${token.value}:${tokenId}:${owner.value}"

}