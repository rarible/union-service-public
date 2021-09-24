package com.rarible.protocol.union.dto

import java.math.BigInteger

data class UnionItemIdDto(
    override val blockchain: BlockchainDto,
    val token: UnionAddress,
    val tokenId: BigInteger
) : UnionBlockchainId {

    override val value = "${token.value}:${tokenId}"

}