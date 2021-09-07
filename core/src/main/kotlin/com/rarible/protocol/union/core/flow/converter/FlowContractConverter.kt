package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.union.dto.FlowBlockchainDto
import com.rarible.protocol.union.dto.flow.FlowContract

object FlowContractConverter {

    fun convert(source: String, blockchain: FlowBlockchainDto): FlowContract {
        return FlowContract(blockchain, source)
    }

}

