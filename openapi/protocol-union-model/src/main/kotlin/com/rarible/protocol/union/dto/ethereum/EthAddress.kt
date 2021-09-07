package com.rarible.protocol.union.dto.ethereum

import com.rarible.protocol.union.dto.EthBlockchainDto

data class EthAddress(
    val blockchain: EthBlockchainDto,
    val value: String
) {

    override fun toString(): String {
        return "${blockchain.name}:${value}"
    }

}