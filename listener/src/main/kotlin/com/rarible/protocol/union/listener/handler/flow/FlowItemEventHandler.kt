package com.rarible.protocol.union.listener.handler.flow

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.union.core.converter.flow.FlowUnionItemEventDtoConverter
import com.rarible.protocol.union.dto.UnionItemEventDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FlowItemEventHandler(
    private val producer: RaribleKafkaProducer<UnionItemEventDto>
) : AbstractEventHandler<FlowNftItemEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: FlowNftItemEventDto) {
        logger.debug("Received Flow item event: type={}", event::class.java.simpleName)

        val unionEventDto = FlowUnionItemEventDtoConverter.convert(event)

        val message = KafkaMessage(
            key = event.itemId,
            value = unionEventDto,
            headers = ITEM_EVENT_HEADERS
        )
        producer.send(message).ensureSuccess()
    }
}
