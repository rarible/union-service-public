package com.rarible.protocol.union.core.handler

import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.protocol.union.dto.BlockchainDto

interface BlockchainEventHandler<B, U> : ConsumerEventHandler<B> {

    val blockchain: BlockchainDto

    val handler: IncomingEventHandler<U>

    suspend fun handleSafely(event: B)

}