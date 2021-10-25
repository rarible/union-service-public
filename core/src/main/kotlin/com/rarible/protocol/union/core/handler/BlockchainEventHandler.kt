package com.rarible.protocol.union.core.handler

import com.rarible.protocol.union.dto.BlockchainDto

abstract class BlockchainEventHandler<B, U>(
    val blockchain: BlockchainDto
) : AbstractEventHandler<B>() {

    abstract val handler: IncomingEventHandler<U>

}