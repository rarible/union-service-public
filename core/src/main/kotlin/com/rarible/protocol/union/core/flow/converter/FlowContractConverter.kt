package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionAddress

object FlowContractConverter {

    fun convert(source: String, blockchain: BlockchainDto): UnionAddress {
        return UnionAddress(blockchain, source)
    }

}

