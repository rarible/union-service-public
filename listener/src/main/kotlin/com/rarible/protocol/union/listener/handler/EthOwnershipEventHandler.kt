package com.rarible.protocol.nftorder.listener.handler

import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EthOwnershipEventHandler : AbstractEventHandler<NftOwnershipEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: NftOwnershipEventDto) {
        logger.debug("Received Ethereum Ownership event: type=${event::class.java.simpleName}")
        // TODO - Implement
    }
}