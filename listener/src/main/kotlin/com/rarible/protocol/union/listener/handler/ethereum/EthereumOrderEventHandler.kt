package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.union.core.converter.ethereum.EthUnionOrderEventDtoConverter
import com.rarible.protocol.union.dto.UnionOrderEventDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import org.slf4j.LoggerFactory

class EthereumOrderEventHandler(
    private val producer: RaribleKafkaProducer<UnionOrderEventDto>,
    private val blockchain: Blockchain
) : AbstractEventHandler<OrderEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: OrderEventDto) {
        logger.debug("Received Ethereum ({}) Order event: type={}", blockchain, event::class.java.simpleName)

        val unionEventDto = EthUnionOrderEventDtoConverter.convert(event, blockchain)

        val message = KafkaMessage(
            key = event.orderId,
            value = unionEventDto,
            headers = ORDER_EVENT_HEADERS
        )
        producer.send(message)
    }
}
