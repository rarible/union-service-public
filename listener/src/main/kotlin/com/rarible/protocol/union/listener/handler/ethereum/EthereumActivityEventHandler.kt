package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.ethereum.converter.EthActivityConverter
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.event.KafkaEventFactory
import com.rarible.protocol.union.listener.handler.BlockchainEventHandler
import org.slf4j.LoggerFactory

class EthereumActivityEventHandler(
    override val blockchain: BlockchainDto,
    private val producer: RaribleKafkaProducer<ActivityDto>,
    private val ethActivityConverter: EthActivityConverter
) : BlockchainEventHandler<com.rarible.protocol.dto.ActivityDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: com.rarible.protocol.dto.ActivityDto) {
        logger.debug("Received Ethereum ({}) Activity event: type={}", blockchain, event::class.java.simpleName)
        val unionEventDto = ethActivityConverter.convert(event, blockchain)
        producer.send(KafkaEventFactory.activityEvent(unionEventDto)).ensureSuccess()
    }
}
