package com.rarible.protocol.union.core.service.router

import com.rarible.protocol.union.dto.BlockchainDto

abstract class AbstractBlockchainService(
    override val blockchain: BlockchainDto
) : BlockchainService
