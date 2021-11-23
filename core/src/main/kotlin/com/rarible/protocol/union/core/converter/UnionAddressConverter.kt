package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.group

object UnionAddressConverter {

    fun convert(blockchain: BlockchainDto, source: String): UnionAddress {
        ContractAddressConverter.validate(blockchain, source)
        return UnionAddress(blockchain.group(), source)
    }

}

