package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.dto.BlockchainDto
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
        return ethOrderConverter.convert(event, blockchain)
    }
}

open class EthereumOrderEventHandler(
    handler: IncomingEventHandler<UnionOrderEvent>, ethOrderConverter: EthOrderConverter
) : EthOrderEventHandler(BlockchainDto.ETHEREUM, handler, ethOrderConverter)

open class PolygonOrderEventHandler(
    handler: IncomingEventHandler<UnionOrderEvent>, ethOrderConverter: EthOrderConverter
) : EthOrderEventHandler(BlockchainDto.POLYGON, handler, ethOrderConverter)