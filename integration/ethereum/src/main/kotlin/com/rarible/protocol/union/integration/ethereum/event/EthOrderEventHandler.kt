package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.protocol.dto.AmmOrderNftUpdateEventDto
import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.union.core.event.EventType
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
) : AbstractBlockchainEventHandler<OrderEventDto, UnionOrderEvent>(
    blockchain,
    EventType.ORDER
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: OrderEventDto): UnionOrderEvent {
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
) : EthOrderEventHandler(BlockchainDto.ETHEREUM, handler, ethOrderConverter)

open class PolygonOrderEventHandler(
    handler: IncomingEventHandler<UnionOrderEvent>, ethOrderConverter: EthOrderConverter
) : EthOrderEventHandler(BlockchainDto.POLYGON, handler, ethOrderConverter)