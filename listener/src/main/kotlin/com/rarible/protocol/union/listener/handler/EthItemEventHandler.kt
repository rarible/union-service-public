package com.rarible.protocol.union.listener.handler

import com.rarible.protocol.dto.NftItemEventDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EthItemEventHandler : AbstractEventHandler<NftItemEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: NftItemEventDto) {
        logger.debug("Received Ethereum Item event: type=${event::class.java.simpleName}")
        // TODO - Implement
    }
}