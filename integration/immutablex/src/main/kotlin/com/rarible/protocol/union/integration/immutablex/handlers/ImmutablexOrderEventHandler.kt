package com.rarible.protocol.union.integration.immutablex.handlers

import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.immutablex.converter.ImmutablexOrderConverter
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexOrder

class ImmutablexOrderEventHandler(
    override val handler: IncomingEventHandler<UnionOrderEvent>
) : AbstractBlockchainEventHandler<ImmutablexOrder, UnionOrderEvent>(BlockchainDto.IMMUTABLEX) {

    override suspend fun handle(event: ImmutablexOrder) {
        val order = ImmutablexOrderConverter.convert(event, blockchain)
        handler.onEvent(UnionOrderUpdateEvent(order))
    }
}
