package com.rarible.protocol.union.listener.handler.flow

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.union.core.flow.converter.FlowUnionItemEventConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionItemEventDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import org.slf4j.LoggerFactory

class FlowItemEventHandler(
    private val producer: RaribleKafkaProducer<UnionItemEventDto>,
    private val blockchain: BlockchainDto
) : AbstractEventHandler<FlowNftItemEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: FlowNftItemEventDto) {
        logger.debug("Received Flow item event: type={}", event::class.java.simpleName)

        val unionEventDto = FlowUnionItemEventConverter.convert(event, blockchain)

        val message = KafkaMessage(
            key = event.itemId,
            value = unionEventDto,
            headers = ITEM_EVENT_HEADERS
        )
        producer.send(message).ensureSuccess()
    }
}
