package com.rarible.protocol.union.listener.handler.flow

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.*
import com.rarible.protocol.union.core.converter.flow.FlowUnionItemEventDtoConverter
import com.rarible.protocol.union.core.misc.toItemId
import com.rarible.protocol.union.dto.UnionItemEventDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import com.rarible.protocol.union.listener.handler.ITEM_EVENT_HEADERS
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FlowItemEventHandler(
    private val producer: RaribleKafkaProducer<UnionItemEventDto>
) : AbstractEventHandler<FlowNftItemEventDto>() {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: FlowNftItemEventDto) {
        logger.debug("Received flow item event: type=${event::class.java.simpleName}")
        val unionEventDto = FlowUnionItemEventDtoConverter.convert(event)

        val message = KafkaMessage(
            key = event.key,
            value = unionEventDto,
            headers = ITEM_EVENT_HEADERS,
            id = event.eventId
        )
        producer.send(message).ensureSuccess()
    }

    private val FlowNftItemEventDto.key: String
        get() = when (this) {
            is FlowNftItemUpdateEventDto -> toItemId(item.contract ?: "", item.tokenId ?: -1) //TODO: Must not be nullable
            is FlowNftItemDeleteEventDto -> toItemId(item.token, item.tokenId)
        }
}
