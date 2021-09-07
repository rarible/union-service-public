package com.rarible.protocol.union.dto.flow

import com.rarible.protocol.union.dto.FlowBlockchainDto

data class FlowAddress(
    val blockchain: FlowBlockchainDto,
    val value: String
) {

    override fun toString(): String {
        return "${blockchain.name}:${value}"
    }

}