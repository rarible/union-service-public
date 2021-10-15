package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.ethereum.converter.EthActivityConverter
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import org.slf4j.LoggerFactory

class EthereumActivityEventHandler(
    private val producer: RaribleKafkaProducer<ActivityDto>,
    private val blockchain: BlockchainDto
) : AbstractEventHandler<com.rarible.protocol.dto.ActivityDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: com.rarible.protocol.dto.ActivityDto) {
        logger.debug("Received Ethereum ({}) Activity event: type={}", blockchain, event::class.java.simpleName)

        val unionEventDto = EthActivityConverter.convert(event, blockchain)

        val message = KafkaMessage(
            key = unionEventDto.id.fullId(), // TODO we need to use right key here
            value = unionEventDto,
            headers = ACTIVITY_EVENT_HEADERS
        )
        producer.send(message).ensureSuccess()
    }
}
