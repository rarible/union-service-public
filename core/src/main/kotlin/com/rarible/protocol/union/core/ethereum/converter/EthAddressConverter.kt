package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.dto.ethereum.EthAddress
import scalether.domain.Address

object EthAddressConverter {
    fun convert(source: Address, blockchain: EthBlockchainDto): EthAddress {
        return EthAddress(blockchain, EthConverter.convert(source))
    }
}

