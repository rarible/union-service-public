package com.rarible.protocol.union.listener.handler.flow

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.union.core.flow.converter.FlowOrderEventConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OrderUpdateEventDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import com.rarible.protocol.union.listener.service.EnrichmentOrderEventService
import org.slf4j.LoggerFactory

class FlowOrderEventHandler(
    private val producer: RaribleKafkaProducer<OrderEventDto>,
    private val orderEventService: EnrichmentOrderEventService,
    private val blockchain: BlockchainDto
) : AbstractEventHandler<FlowOrderEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: FlowOrderEventDto) {
        logger.debug("Received Flow Order event: type={}", event::class.java.simpleName)

        val unionEventDto = FlowOrderEventConverter.convert(event, blockchain)

        when (unionEventDto) {
            is OrderUpdateEventDto -> orderEventService.updateOrder(unionEventDto.order)
        }

        val message = KafkaMessage(
            key = unionEventDto.orderId.fullId(),
            value = unionEventDto as OrderEventDto,
            headers = ORDER_EVENT_HEADERS
        )
        producer.send(message)

    }
}
