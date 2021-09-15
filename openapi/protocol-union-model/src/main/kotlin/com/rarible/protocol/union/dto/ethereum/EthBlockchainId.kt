package com.rarible.protocol.union.dto.ethereum

import com.rarible.protocol.union.dto.BlockchainId
import com.rarible.protocol.union.dto.EthBlockchainDto

abstract class EthBlockchainId : BlockchainId {

    abstract val blockchain: EthBlockchainDto

    override fun blockchainName() = blockchain.name

}