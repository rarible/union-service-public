package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.dto.AmmOrderNftUpdateEventDto
import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.core.model.UnionPoolNftUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import org.slf4j.LoggerFactory

abstract class EthOrderEventHandler(
    blockchain: BlockchainDto,
    override val handler: IncomingEventHandler<UnionOrderEvent>,
    private val ethOrderConverter: EthOrderConverter
) : AbstractBlockchainEventHandler<OrderEventDto, UnionOrderEvent>(blockchain) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun handleInternal(event: OrderEventDto) = handler.onEvent(convert(event))
    suspend fun handleInternal(events: List<OrderEventDto>) = handler.onEvents(events.map { convert(it) })

    private suspend fun convert(event: OrderEventDto): UnionOrderEvent {
        logger.info("Received {} Order event: {}", blockchain, event)

        return when (event) {
            is com.rarible.protocol.dto.OrderUpdateEventDto -> {
                val order = ethOrderConverter.convert(event.order, blockchain)
                UnionOrderUpdateEvent(order)
            }
            is AmmOrderNftUpdateEventDto -> {
                val orderId = OrderIdDto(blockchain, event.orderId)
                val include = event.inNft.map { ItemIdDto(blockchain, it) }
                val exclude = event.outNft.map { ItemIdDto(blockchain, it) }
                UnionPoolNftUpdateEvent(orderId, include.toSet(), exclude.toSet())
            }
        }
    }
}

open class EthereumOrderEventHandler(
    handler: IncomingEventHandler<UnionOrderEvent>, ethOrderConverter: EthOrderConverter
) : EthOrderEventHandler(BlockchainDto.ETHEREUM, handler, ethOrderConverter) {

    @CaptureTransaction("OrderEvent#ETHEREUM")
    override suspend fun handle(event: OrderEventDto) = handleInternal(event)

    @CaptureTransaction("OrderEvents#ETHEREUM")
    override suspend fun handle(events: List<OrderEventDto>) = handleInternal(events)
}

open class PolygonOrderEventHandler(
    handler: IncomingEventHandler<UnionOrderEvent>, ethOrderConverter: EthOrderConverter
) : EthOrderEventHandler(BlockchainDto.POLYGON, handler, ethOrderConverter) {

    @CaptureTransaction("OrderEvent#POLYGON")
    override suspend fun handle(event: OrderEventDto) = handleInternal(event)

    @CaptureTransaction("OrderEvents#POLYGON")
    override suspend fun handle(events: List<OrderEventDto>) = handleInternal(events)
}