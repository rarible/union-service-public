package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.ethereum.converter.EthOrderEventConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OrderUpdateEventDto
import com.rarible.protocol.union.enrichment.service.event.OrderEventService
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import org.slf4j.LoggerFactory

class EthereumOrderEventHandler(
    private val producer: RaribleKafkaProducer<OrderEventDto>,
    private val orderEventService: OrderEventService,
    private val blockchain: BlockchainDto
) : AbstractEventHandler<com.rarible.protocol.dto.OrderEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: com.rarible.protocol.dto.OrderEventDto) {
        logger.debug("Received Ethereum ({}) Order event: type={}", blockchain, event::class.java.simpleName)

        val unionEventDto = EthOrderEventConverter.convert(event, blockchain)

        val message = KafkaMessage(
            key = unionEventDto.orderId.fullId(),
            value = unionEventDto,
            headers = ORDER_EVENT_HEADERS
        )
        producer.send(message)

        // TODO maybe it should be called first?
        when (unionEventDto) {
            is OrderUpdateEventDto -> orderEventService.updateOrder(unionEventDto.order)
        }
    }
}
