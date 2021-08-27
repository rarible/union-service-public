package com.rarible.protocol.union.listener.handler.flow

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.FlowOwnershipEventDto
import com.rarible.protocol.dto.UnionEventTopicProvider
import com.rarible.protocol.union.dto.UnionOwnershipEventDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FlowOwnershipEventHandler(
    private val producer: RaribleKafkaProducer<UnionOwnershipEventDto>
) : AbstractEventHandler<FlowOwnershipEventDto>() {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val ownershipEventHeaders = mapOf("protocol.union.ownership.event.version" to UnionEventTopicProvider.VERSION)

    override suspend fun handleSafely(event: FlowOwnershipEventDto) {
        logger.debug("Received flow ownership event: type=${event::class.java.simpleName}")
        TODO()
    }
}
