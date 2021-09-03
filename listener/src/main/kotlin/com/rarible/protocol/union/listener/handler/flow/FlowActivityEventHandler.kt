package com.rarible.protocol.union.listener.handler.flow

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.union.core.converter.flow.FlowUnionActivityDtoConverter
import com.rarible.protocol.union.dto.UnionActivityDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FlowActivityEventHandler(
    private val producer: RaribleKafkaProducer<UnionActivityDto>
) : AbstractEventHandler<FlowActivityDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: FlowActivityDto) {
        logger.debug("Received Flow ({}) Activity event: type={}", event::class.java.simpleName)

        val unionEventDto = FlowUnionActivityDtoConverter.convert(event)

        val message = KafkaMessage(
            key = event.id,
            value = unionEventDto,
            headers = ACTIVITY_EVENT_HEADERS
        )
        producer.send(message).ensureSuccess()
    }
}
