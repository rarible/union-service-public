package com.rarible.protocol.union.integration.immutablex.handlers

import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.L2DepositActivityDto
import com.rarible.protocol.union.dto.L2WithdrawalActivityDto
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexEvent
import com.rarible.protocol.union.integration.immutablex.converter.ImmutablexEventConverter

class ImmutablexActivityEventHandler(
    override val handler: IncomingEventHandler<ActivityDto>,
    private val converter: ImmutablexEventConverter
) : AbstractBlockchainEventHandler<ImmutablexEvent, ActivityDto>(BlockchainDto.IMMUTABLEX) {
    override suspend fun handle(event: ImmutablexEvent) {
        when (val dto = converter.convert(event)) {
            // We don't need these events ATM
            is L2DepositActivityDto -> return
            is L2WithdrawalActivityDto -> return
            else -> handler.onEvent(dto)
        }
    }
}
