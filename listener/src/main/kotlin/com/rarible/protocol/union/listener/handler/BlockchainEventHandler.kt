package com.rarible.protocol.union.listener.handler

import com.rarible.protocol.union.dto.BlockchainDto

abstract class BlockchainEventHandler<T> : AbstractEventHandler<T>() {

    abstract val blockchain: BlockchainDto

}