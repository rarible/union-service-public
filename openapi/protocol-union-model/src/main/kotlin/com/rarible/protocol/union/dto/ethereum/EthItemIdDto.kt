package com.rarible.protocol.union.dto.ethereum

import com.rarible.protocol.union.dto.EthBlockchainDto
import java.math.BigInteger

data class EthItemIdDto(
    override val blockchain: EthBlockchainDto,
    val token: EthAddress,
    val tokenId: BigInteger
) : EthBlockchainId() {

    override val value = "${token.value}:${tokenId}"

}