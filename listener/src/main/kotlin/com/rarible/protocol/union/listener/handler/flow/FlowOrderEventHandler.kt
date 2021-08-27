package com.rarible.protocol.union.listener.handler.flow

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.*
import com.rarible.protocol.union.dto.UnionOrderEventDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FlowOrderEventHandler(
    private val producer: RaribleKafkaProducer<UnionOrderEventDto>
) : AbstractEventHandler<FlowOrderEventDto>() {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val orderEventHeaders = mapOf("protocol.union.order.event.version" to UnionEventTopicProvider.VERSION)

    override suspend fun handleSafely(event: FlowOrderEventDto) {
        logger.debug("Received flow order event: type=${event::class.java.simpleName}")
        TODO()
    }
}
