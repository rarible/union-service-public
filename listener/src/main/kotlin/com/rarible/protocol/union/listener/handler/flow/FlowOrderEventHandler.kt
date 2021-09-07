package com.rarible.protocol.union.listener.handler.flow

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.union.core.flow.converter.FlowUnionOrderEventConverter
import com.rarible.protocol.union.dto.FlowBlockchainDto
import com.rarible.protocol.union.dto.UnionOrderEventDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import org.slf4j.LoggerFactory

class FlowOrderEventHandler(
    private val producer: RaribleKafkaProducer<UnionOrderEventDto>,
    private val blockchain: FlowBlockchainDto
) : AbstractEventHandler<FlowOrderEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: FlowOrderEventDto) {
        logger.debug("Received Flow Order event: type={}", event::class.java.simpleName)

        val unionEventDto = FlowUnionOrderEventConverter.convert(event, blockchain)

        val message = KafkaMessage(
            key = event.orderId,
            value = unionEventDto,
            headers = ORDER_EVENT_HEADERS
        )
        producer.send(message)
    }
}
