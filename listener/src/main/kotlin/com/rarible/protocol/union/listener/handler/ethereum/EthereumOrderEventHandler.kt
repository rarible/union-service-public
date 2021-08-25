package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.*
import com.rarible.protocol.union.core.converter.ethereum.UnionOrderEventDtoConverter
import com.rarible.protocol.union.dto.UnionOrderEventDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import org.slf4j.LoggerFactory
import scalether.domain.Address
import java.math.BigInteger

class EthereumOrderEventHandler(
    private val blockchain: Blockchain,
    private val producer: RaribleKafkaProducer<UnionOrderEventDto>
) : AbstractEventHandler<OrderEventDto>() {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val orderEventHeaders = mapOf("protocol.union.order.event.version" to UnionEventTopicProvider.VERSION)

    override suspend fun handleSafely(event: OrderEventDto) {
        logger.debug("Received ${blockchain.value} Order event: type=${event::class.java.simpleName}")

        val key = when (event) {
            is OrderUpdateEventDto ->
                event.order.key ?: event.orderId
        }
        val unionEventDto = UnionOrderEventDtoConverter.convert(event, blockchain)

        val message = KafkaMessage(
            key = key,
            value = unionEventDto,
            headers = orderEventHeaders,
            id = event.eventId
        )
        producer.send(message)
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

    private fun toItemId(contract: Address, tokenId: BigInteger) = "$contract:$tokenId"
}
