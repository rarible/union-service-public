package com.rarible.protocol.union.core.ethereum.service

import com.rarible.protocol.union.core.service.BlockchainService
import com.rarible.protocol.union.dto.BlockchainDto

abstract class AbstractEthereumService(
    override val blockchain: BlockchainDto
) : BlockchainService