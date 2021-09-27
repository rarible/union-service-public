package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.core.ethereum.converter.EthConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionAddress
import scalether.domain.Address

object UnionAddressConverter {

    fun convert(source: Address, blockchain: BlockchainDto): UnionAddress {
        return UnionAddress(blockchain, EthConverter.convert(source))
    }

    fun convert(source: String, blockchain: BlockchainDto): UnionAddress {
        return UnionAddress(blockchain, source)
    }

}

