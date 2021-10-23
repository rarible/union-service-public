package com.rarible.protocol.union.listener.handler.tezos

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.tezos.converter.TezosActivityConverter
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.event.KafkaEventFactory
import com.rarible.protocol.union.listener.handler.BlockchainEventHandler
import org.slf4j.LoggerFactory

class TezosActivityEventHandler(
    override val blockchain: BlockchainDto,
    private val producer: RaribleKafkaProducer<ActivityDto>,
    private val tezosActivityConverter: TezosActivityConverter
) : BlockchainEventHandler<com.rarible.protocol.tezos.dto.ActivityDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: com.rarible.protocol.tezos.dto.ActivityDto) {
        logger.debug("Received Tezos ({}) Activity event: type={}", blockchain, event::class.java.simpleName)
        // if type == null, it means event unparseable - will be logged inside of parser
        if (event.type != null) {
            val unionEventDto = tezosActivityConverter.convert(event.type!!, blockchain)
            producer.send(KafkaEventFactory.activityEvent(unionEventDto)).ensureSuccess()
        }
    }
}
