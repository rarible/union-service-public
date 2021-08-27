package com.rarible.protocol.union.listener.handler.flow

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.*
import com.rarible.protocol.union.dto.UnionItemEventDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FlowItemEventHandler(
    private val producer: RaribleKafkaProducer<UnionItemEventDto>
) : AbstractEventHandler<FlowNftItemEventDto>() {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val itemEventHeaders = mapOf("protocol.union.item.event.version" to UnionEventTopicProvider.VERSION)

    override suspend fun handleSafely(event: FlowNftItemEventDto) {
        logger.debug("Received flow item event: type=${event::class.java.simpleName}")
        TODO()
    }
}
