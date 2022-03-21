package com.rarible.protocol.union.integration.immutablex.events

import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.integration.immutablex.converter.ImmutablexEventConverter
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexEvent


class ImmutablexActivityEventHandler(
    override val handler: IncomingEventHandler<ActivityDto>,
    private val converter: ImmutablexEventConverter
) : AbstractBlockchainEventHandler<ImmutablexEvent, ActivityDto>(BlockchainDto.IMMUTABLEX) {
    override suspend fun handle(event: ImmutablexEvent) {
        val dto = converter.convert(event)
        handler.onEvent(dto)
    }
}
