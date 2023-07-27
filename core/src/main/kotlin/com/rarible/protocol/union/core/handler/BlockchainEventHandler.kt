package com.rarible.protocol.union.core.handler

import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.dto.BlockchainDto

interface BlockchainEventHandler<B, U> {

    val blockchain: BlockchainDto

    val eventType: EventType

    val handler: IncomingEventHandler<U>

    suspend fun handle(event: B)

    suspend fun handle(events: List<B>)
}
