package com.rarible.protocol.union.core.flow.service

import com.rarible.protocol.union.core.service.router.BlockchainService
import com.rarible.protocol.union.dto.BlockchainDto

abstract class AbstractFlowService(
    override val blockchain: BlockchainDto
) : BlockchainService