package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import org.slf4j.LoggerFactory

abstract class EthOrderEventHandler(
    blockchain: BlockchainDto,
    override val handler: IncomingEventHandler<UnionOrderEvent>,
    private val ethOrderConverter: EthOrderConverter
) : AbstractBlockchainEventHandler<OrderEventDto, UnionOrderEvent>(blockchain) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun handleInternal(event: OrderEventDto) {
        logger.info("Received {} Order event: {}", blockchain, event)

        when (event) {
            is com.rarible.protocol.dto.OrderUpdateEventDto -> {
                val order = ethOrderConverter.convert(event.order, blockchain)
                val unionEventDto = UnionOrderUpdateEvent(order)
                handler.onEvent(unionEventDto)
            }
        }
    }
}

open class EthereumOrderEventHandler(
    handler: IncomingEventHandler<UnionOrderEvent>, ethOrderConverter: EthOrderConverter
) : EthOrderEventHandler(BlockchainDto.ETHEREUM, handler, ethOrderConverter) {
    @CaptureTransaction("OrderEvent#ETHEREUM")
    override suspend fun handle(event: OrderEventDto) = handleInternal(event)
}

open class PolygonOrderEventHandler(
    handler: IncomingEventHandler<UnionOrderEvent>, ethOrderConverter: EthOrderConverter
) : EthOrderEventHandler(BlockchainDto.POLYGON, handler, ethOrderConverter) {
    @CaptureTransaction("OrderEvent#POLYGON")
    override suspend fun handle(event: OrderEventDto) = handleInternal(event)
}