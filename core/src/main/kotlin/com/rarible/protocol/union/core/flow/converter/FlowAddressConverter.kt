package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.union.dto.FlowBlockchainDto
import com.rarible.protocol.union.dto.flow.FlowAddress

object FlowAddressConverter {

    fun convert(source: String, blockchain: FlowBlockchainDto): FlowAddress {
        return FlowAddress(blockchain, source)
    }

}

