package com.rarible.protocol.union.dto.ethereum

import com.rarible.protocol.union.dto.EthBlockchainDto
import java.math.BigInteger

data class EthOwnershipIdDto(
    override val blockchain: EthBlockchainDto,
    val token: EthAddress,
    val tokenId: BigInteger,
    val owner: EthAddress
) : EthBlockchainId() {

    override val value = "${token.value}:${tokenId}:${owner.value}"

}