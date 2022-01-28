package com.rarible.protocol.union.integration.tezos.event

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.tezos.dto.TezosOrderSafeEventDto
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.tezos.converter.TezosOrderConverter
import org.slf4j.LoggerFactory

open class TezosOrderEventHandler(
    override val handler: IncomingEventHandler<UnionOrderEvent>,
    private val tezosOrderConverter: TezosOrderConverter
) : AbstractBlockchainEventHandler<TezosOrderSafeEventDto, UnionOrderEvent>(BlockchainDto.TEZOS) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @CaptureTransaction("OrderEvent#TEZOS")
    override suspend fun handle(event: TezosOrderSafeEventDto) {
        // TODO make this event as data class
        logger.info("Received {} Order event: type = {}, order = {}", blockchain, event.type, event.order)

        when (event.type) {
            TezosOrderSafeEventDto.Type.UPDATE -> {
                val unionOrder = tezosOrderConverter.convert(event.order!!, blockchain)
                handler.onEvent(UnionOrderUpdateEvent(unionOrder))
            }
            TezosOrderSafeEventDto.Type.SERIALIZATION_FAILED -> {
                // skip it, will be logged inside of parser
            }
        }

    }
}
