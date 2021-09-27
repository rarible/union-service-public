package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.union.core.ethereum.converter.EthUnionOwnershipEventConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionOwnershipEventDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import org.slf4j.LoggerFactory

class EthereumOwnershipEventHandler(
    private val producer: RaribleKafkaProducer<UnionOwnershipEventDto>,
    private val blockchain: BlockchainDto
) : AbstractEventHandler<NftOwnershipEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: NftOwnershipEventDto) {
        logger.debug("Received Ethereum ({}) Ownership event: type={}", blockchain, event::class.java.simpleName)

        val unionEventDto = EthUnionOwnershipEventConverter.convert(event, blockchain)

        val message = KafkaMessage(
            key = event.ownershipId,
            value = unionEventDto,
            headers = OWNERSHIP_EVENT_HEADERS
        )
        producer.send(message).ensureSuccess()
    }
}
