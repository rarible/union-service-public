package com.rarible.protocol.union.listener.handler.flow

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.union.core.flow.converter.FlowUnionActivityConverter
import com.rarible.protocol.union.dto.FlowBlockchainDto
import com.rarible.protocol.union.dto.UnionActivityDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import org.slf4j.LoggerFactory

class FlowActivityEventHandler(
    private val producer: RaribleKafkaProducer<UnionActivityDto>,
    private val blockchain: FlowBlockchainDto
) : AbstractEventHandler<FlowActivityDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: FlowActivityDto) {
        logger.debug("Received Flow ({}) Activity event: type={}", event::class.java.simpleName)

        val unionEventDto = FlowUnionActivityConverter.convert(event, blockchain)

        val message = KafkaMessage(
            key = event.id,
            value = unionEventDto,
            headers = ACTIVITY_EVENT_HEADERS
        )
        producer.send(message).ensureSuccess()
    }
}
