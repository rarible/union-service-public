package com.rarible.protocol.union.listener.handler.flow

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOrderUpdateEventDto
import com.rarible.protocol.union.core.flow.converter.FlowOrderConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OrderUpdateEventDto
import com.rarible.protocol.union.enrichment.event.KafkaEventFactory
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import com.rarible.protocol.union.listener.service.EnrichmentOrderEventService
import org.slf4j.LoggerFactory

class FlowOrderEventHandler(
    private val producer: RaribleKafkaProducer<OrderEventDto>,
    private val orderEventService: EnrichmentOrderEventService,
    private val flowOrderConverter: FlowOrderConverter,
    private val blockchain: BlockchainDto
) : AbstractEventHandler<FlowOrderEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: FlowOrderEventDto) {
        logger.debug("Received Flow Order event: type={}", event::class.java.simpleName)

        when (event) {
            is FlowOrderUpdateEventDto -> {
                val order = flowOrderConverter.convert(event.order, blockchain)
                val unionEventDto = OrderUpdateEventDto(
                    eventId = event.eventId,
                    orderId = order.id,
                    order = order
                )

                orderEventService.updateOrder(unionEventDto.order)

                producer.send(KafkaEventFactory.orderEvent(unionEventDto))
            }
        }
    }
}
