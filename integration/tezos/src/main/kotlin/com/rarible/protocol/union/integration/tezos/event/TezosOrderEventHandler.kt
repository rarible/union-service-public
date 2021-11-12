package com.rarible.protocol.union.integration.tezos.event

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.tezos.dto.OrderEventDto
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.tezos.converter.TezosOrderConverter
import org.slf4j.LoggerFactory

class TezosOrderEventHandler(
    override val handler: IncomingEventHandler<UnionOrderEvent>,
    private val tezosOrderConverter: TezosOrderConverter
) : AbstractBlockchainEventHandler<OrderEventDto, UnionOrderEvent>(BlockchainDto.TEZOS) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @CaptureTransaction("OrderEvent#TEZOS")
    override suspend fun handleSafely(event: com.rarible.protocol.tezos.dto.OrderEventDto) {
        logger.info("Received Tezos Order event: type={}", event::class.java.simpleName)

        when (event.type) {
            com.rarible.protocol.tezos.dto.OrderEventDto.Type.UPDATE -> {
                val unionOrder = tezosOrderConverter.convert(event.order!!, blockchain)
                handler.onEvent(UnionOrderUpdateEvent(unionOrder))
            }
            com.rarible.protocol.tezos.dto.OrderEventDto.Type.SERIALIZATION_FAILED -> {
                // skip it, will be logged inside of parser
            }
        }

    }
}
