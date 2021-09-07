package com.rarible.protocol.union.listener.handler.flow

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.FlowOwnershipEventDto
import com.rarible.protocol.union.core.flow.converter.FlowUnionOwnershipEventConverter
import com.rarible.protocol.union.dto.FlowBlockchainDto
import com.rarible.protocol.union.dto.UnionOwnershipEventDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import org.slf4j.LoggerFactory

class FlowOwnershipEventHandler(
    private val producer: RaribleKafkaProducer<UnionOwnershipEventDto>,
    private val blockchain: FlowBlockchainDto
) : AbstractEventHandler<FlowOwnershipEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: FlowOwnershipEventDto) {
        logger.debug("Received Flow Ownership event: type={}", event::class.java.simpleName)

        val unionEventDto = FlowUnionOwnershipEventConverter.convert(event, blockchain)

        val message = KafkaMessage(
            key = event.ownershipId,
            value = unionEventDto,
            headers = OWNERSHIP_EVENT_HEADERS
        )
        producer.send(message).ensureSuccess()
    }
}
