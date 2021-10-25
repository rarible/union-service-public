package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionAddress

object UnionAddressConverter {

    fun convert(source: String, blockchain: BlockchainDto): UnionAddress {
        return UnionAddress(blockchain, source)
    }

}

