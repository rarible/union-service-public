package com.rarible.protocol.union.listener.handler.tezos

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.tezos.converter.TezosOrderConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OrderUpdateEventDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import com.rarible.protocol.union.listener.service.EnrichmentOrderEventService
import org.slf4j.LoggerFactory

class TezosOrderEventHandler(
    private val producer: RaribleKafkaProducer<OrderEventDto>,
    private val orderEventService: EnrichmentOrderEventService,
    private val tezosOrderConverter: TezosOrderConverter,
    private val blockchain: BlockchainDto
) : AbstractEventHandler<com.rarible.protocol.tezos.dto.OrderEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: com.rarible.protocol.tezos.dto.OrderEventDto) {
        logger.info("Received Tezos Order event: type={}", event::class.java.simpleName)

        when (event.type) {
            com.rarible.protocol.tezos.dto.OrderEventDto.Type.UPDATE -> {
                val unionOrder = tezosOrderConverter.convert(event.order!!, blockchain)
                orderEventService.updateOrder(unionOrder)

                val unionEventDto = OrderUpdateEventDto(
                    orderId = unionOrder.id,
                    eventId = event.eventId!!,
                    order = unionOrder
                )

                val message = KafkaMessage(
                    key = unionOrder.id.fullId(),
                    value = unionEventDto as OrderEventDto,
                    headers = ORDER_EVENT_HEADERS
                )
                producer.send(message)
            }
            com.rarible.protocol.tezos.dto.OrderEventDto.Type.SERIALIZATION_FAILED -> {
                // skip it, will be logged inside of parser
            }
        }

    }
}
