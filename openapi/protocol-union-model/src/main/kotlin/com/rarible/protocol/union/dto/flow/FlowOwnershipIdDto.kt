package com.rarible.protocol.union.dto.flow

import com.rarible.protocol.union.dto.FlowBlockchainDto
import java.math.BigInteger

data class FlowOwnershipIdDto(
    override val blockchain: FlowBlockchainDto,
    val token: FlowContract,
    val tokenId: BigInteger,
    val owner: FlowAddress
) : FlowBlockchainId() {

    override val value = "${token.value}:${tokenId}:${owner.value}"

}