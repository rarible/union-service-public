package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.ActivityDto
import com.rarible.protocol.union.core.ethereum.converter.EthUnionActivityConverter
import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.dto.UnionActivityDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import org.slf4j.LoggerFactory

class EthereumActivityEventHandler(
    private val producer: RaribleKafkaProducer<UnionActivityDto>,
    private val blockchain: EthBlockchainDto
) : AbstractEventHandler<ActivityDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: ActivityDto) {
        logger.debug("Received Ethereum ({}) Activity event: type={}", blockchain, event::class.java.simpleName)

        val unionEventDto = EthUnionActivityConverter.convert(event, blockchain)

        val message = KafkaMessage(
            key = event.id,
            value = unionEventDto,
            headers = ACTIVITY_EVENT_HEADERS
        )
        producer.send(message).ensureSuccess()
    }
}
