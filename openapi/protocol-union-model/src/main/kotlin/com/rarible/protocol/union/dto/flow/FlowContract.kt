package com.rarible.protocol.union.dto.flow

import com.rarible.protocol.union.dto.FlowBlockchainDto

data class FlowContract(
    val blockchain: FlowBlockchainDto,
    val value: String
) {

    override fun toString(): String {
        return "${blockchain.name}:${value}"
    }
}
