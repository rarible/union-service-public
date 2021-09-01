package com.rarible.protocol.union.listener.handler.flow

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.FlowOwnershipEventDto
import com.rarible.protocol.union.core.converter.flow.FlowUnionOwnershipEventDtoConverter
import com.rarible.protocol.union.dto.UnionOwnershipEventDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import com.rarible.protocol.union.listener.handler.OWNERSHIP_EVENT_HEADERS
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FlowOwnershipEventHandler(
    private val producer: RaribleKafkaProducer<UnionOwnershipEventDto>
) : AbstractEventHandler<FlowOwnershipEventDto>() {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: FlowOwnershipEventDto) {
        logger.debug("Received Flow Ownership event: type={}", event::class.java.simpleName)
        val unionEventDto = FlowUnionOwnershipEventDtoConverter.convert(event)

        val message = KafkaMessage(
            key = event.ownershipId,
            value = unionEventDto,
            headers = OWNERSHIP_EVENT_HEADERS
        )
        producer.send(message).ensureSuccess()
    }
}
