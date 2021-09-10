package com.rarible.protocol.union.core.flow.service

import com.rarible.protocol.union.core.service.BlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.FlowBlockchainDto

abstract class AbstractFlowService(
    val blockchain: FlowBlockchainDto
) : BlockchainService {

    private val genericBlockchain = BlockchainDto.valueOf(blockchain.name)

    override fun getBlockchain() = genericBlockchain
}