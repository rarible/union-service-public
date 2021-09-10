package com.rarible.protocol.union.core.ethereum.service

import com.rarible.protocol.union.core.service.BlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EthBlockchainDto

abstract class AbstractEthereumService(
    val blockchain: EthBlockchainDto
) : BlockchainService {

    private val genericBlockchain = BlockchainDto.valueOf(blockchain.name)

    override fun getBlockchain() = genericBlockchain
}