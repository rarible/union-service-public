package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.*
import com.rarible.protocol.union.core.converter.ethereum.UnionOrderEventDtoConverter
import com.rarible.protocol.union.core.misc.toItemId
import com.rarible.protocol.union.dto.UnionOrderEventDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import org.slf4j.LoggerFactory

class EthereumOrderEventHandler(
    private val blockchain: Blockchain,
    private val producer: RaribleKafkaProducer<UnionOrderEventDto>
) : AbstractEventHandler<OrderEventDto>() {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val orderEventHeaders = mapOf("protocol.union.order.event.version" to UnionEventTopicProvider.VERSION)

    override suspend fun handleSafely(event: OrderEventDto) {
        logger.debug("Received ${blockchain.value} Order event: type=${event::class.java.simpleName}")
        val unionEventDto = UnionOrderEventDtoConverter.convert(event, blockchain)

        val message = KafkaMessage(
            key = event.key,
            value = unionEventDto,
            headers = orderEventHeaders,
            id = event.eventId
        )
        producer.send(message)
    }

    private val OrderEventDto.key: String
        get() = when (this) {
            is OrderUpdateEventDto -> order.key ?: orderId
        }

    private val OrderDto.key: String?
        get() = make.assetType.itemId ?: take.assetType.itemId

    private val AssetTypeDto.itemId: String?
        get() = when (this) {
            is Erc721AssetTypeDto -> toItemId(contract, tokenId)
            is Erc1155AssetTypeDto -> toItemId(contract, tokenId)
            is Erc1155LazyAssetTypeDto -> toItemId(contract, tokenId)
            is Erc721LazyAssetTypeDto -> toItemId(contract, tokenId)
            is EthAssetTypeDto, is Erc20AssetTypeDto -> null
            is FlowAssetTypeDto -> throw UnsupportedOperationException("Unsupported assert type ${this.javaClass}") //TODO: I think need to remove from Eth api
        }
}
