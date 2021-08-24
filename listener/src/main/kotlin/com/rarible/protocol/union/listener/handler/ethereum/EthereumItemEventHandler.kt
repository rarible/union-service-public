package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EthereumItemEventHandler() : AbstractEventHandler<NftItemEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: NftItemEventDto) {
        logger.debug("Received Ethereum Item event: type=${event::class.java.simpleName}")
        // TODO - Implement
    }
}
