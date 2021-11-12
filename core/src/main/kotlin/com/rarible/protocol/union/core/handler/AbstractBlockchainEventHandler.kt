package com.rarible.protocol.union.core.handler

import com.rarible.protocol.union.dto.BlockchainDto

abstract class AbstractBlockchainEventHandler<B, U>(
    override val blockchain: BlockchainDto
) : BlockchainEventHandler<B, U>
