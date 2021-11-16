package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.group

object UnionAddressConverter {

    fun convert(blockchain: BlockchainDto, source: String): UnionAddress {
        return UnionAddress(blockchain.group(), source)
    }

}

