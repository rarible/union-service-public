package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import org.slf4j.LoggerFactory

class EthOrderEventHandler(
    blockchain: BlockchainDto,
    override val handler: IncomingEventHandler<UnionOrderEvent>,
    private val ethOrderConverter: EthOrderConverter
) : BlockchainEventHandler<com.rarible.protocol.dto.OrderEventDto, UnionOrderEvent>(blockchain) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: com.rarible.protocol.dto.OrderEventDto) {
        logger.debug("Received Ethereum ({}) Order event: type={}", blockchain, event::class.java.simpleName)

        when (event) {
            is com.rarible.protocol.dto.OrderUpdateEventDto -> {
                val order = ethOrderConverter.convert(event.order, blockchain)
                val unionEventDto = UnionOrderUpdateEvent(order)
                handler.onEvent(unionEventDto)
            }
        }
    }
}
