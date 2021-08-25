package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.dto.UnionEventTopicProvider
import com.rarible.protocol.union.core.converter.ethereum.UnionOwnershipEventDtoConverter
import com.rarible.protocol.union.core.misc.toItemId
import com.rarible.protocol.union.dto.UnionOwnershipEventDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import org.slf4j.LoggerFactory

class EthereumOwnershipEventHandler(
    private val blockchain: Blockchain,
    private val producer: RaribleKafkaProducer<UnionOwnershipEventDto>
) : AbstractEventHandler<NftOwnershipEventDto>() {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val ownershipEventHeaders = mapOf("protocol.union.ownership.event.version" to UnionEventTopicProvider.VERSION)

    override suspend fun handleSafely(event: NftOwnershipEventDto) {
        logger.debug("Received ${blockchain.value} Ownership event: type=${event::class.java.simpleName}")
        val unionEventDto = UnionOwnershipEventDtoConverter.convert(event, blockchain)

        val message = KafkaMessage(
            key = event.key,
            value = unionEventDto,
            headers = ownershipEventHeaders,
            id = event.eventId
        )
        producer.send(message).ensureSuccess()
    }

    private val NftOwnershipEventDto.key: String
        get() = when (this) {
            is NftOwnershipUpdateEventDto -> toItemId(ownership.contract, ownership.tokenId)
            is NftOwnershipDeleteEventDto -> toItemId(ownership.token, ownership.tokenId)
        }
}
