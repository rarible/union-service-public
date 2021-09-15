package com.rarible.protocol.union.dto.flow

import com.rarible.protocol.union.dto.BlockchainId
import com.rarible.protocol.union.dto.FlowBlockchainDto

abstract class FlowBlockchainId : BlockchainId {

    abstract val blockchain: FlowBlockchainDto

    override fun blockchainName() = blockchain.name

}