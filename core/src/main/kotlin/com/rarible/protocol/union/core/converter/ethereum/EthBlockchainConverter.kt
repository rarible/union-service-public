package com.rarible.protocol.union.core.converter.ethereum

import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.union.dto.EthBlockchainDto

object EthBlockchainConverter {

    fun convert(source: Blockchain): EthBlockchainDto {
        return when (source) {
            Blockchain.ETHEREUM -> EthBlockchainDto.ETHEREUM
            Blockchain.POLYGON -> EthBlockchainDto.POLYGON
        }
    }
}