package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.union.dto.UnionItemEventDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import org.slf4j.LoggerFactory

class EthereumItemEventHandler(
    private val blockchain: Blockchain,
    private val producer: RaribleKafkaProducer<UnionItemEventDto>
) : AbstractEventHandler<NftItemEventDto>() {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: NftItemEventDto) {
        logger.debug("Received Ethereum Item event: type=${event::class.java.simpleName}")
        // TODO - Implement
    }
}
