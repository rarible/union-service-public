package com.rarible.protocol.union.listener.handler.flow

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.union.core.converter.flow.FlowUnionOrderEventDtoConverter
import com.rarible.protocol.union.dto.UnionOrderEventDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import com.rarible.protocol.union.listener.handler.ORDER_EVENT_HEADERS
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FlowOrderEventHandler(
    private val producer: RaribleKafkaProducer<UnionOrderEventDto>
) : AbstractEventHandler<FlowOrderEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: FlowOrderEventDto) {
        logger.debug("Received Flow Order event: type={}", event::class.java.simpleName)

        val unionEventDto = FlowUnionOrderEventDtoConverter.convert(event)

        val message = KafkaMessage(
            key = event.orderId,
            value = unionEventDto,
            headers = ORDER_EVENT_HEADERS
        )
        producer.send(message)
    }
}
