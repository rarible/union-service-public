package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.union.core.converter.ethereum.EthUnionItemEventDtoConverter
import com.rarible.protocol.union.dto.UnionItemEventDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import com.rarible.protocol.union.listener.handler.ITEM_EVENT_HEADERS
import org.slf4j.LoggerFactory

class EthereumItemEventHandler(
    private val blockchain: Blockchain,
    private val producer: RaribleKafkaProducer<UnionItemEventDto>
) : AbstractEventHandler<NftItemEventDto>() {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: NftItemEventDto) {
        logger.debug("Received Ethereum ({}) Item event: type={}", blockchain, event::class.java.simpleName)

        val unionEventDto = EthUnionItemEventDtoConverter.convert(event, blockchain)

        val message = KafkaMessage(
            key = event.itemId,
            value = unionEventDto,
            headers = ITEM_EVENT_HEADERS
        )
        producer.send(message).ensureSuccess()
    }
}
