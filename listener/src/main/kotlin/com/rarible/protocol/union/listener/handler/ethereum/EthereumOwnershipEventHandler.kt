package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.union.dto.UnionOwnershipEventDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import org.slf4j.LoggerFactory

class EthereumOwnershipEventHandler(
    private val blockchain: Blockchain,
    private val producer: RaribleKafkaProducer<UnionOwnershipEventDto>
) : AbstractEventHandler<NftOwnershipEventDto>() {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: NftOwnershipEventDto) {
        logger.debug("Received Ethereum Ownership event: type=${event::class.java.simpleName}")
        // TODO - Implement
    }
}
