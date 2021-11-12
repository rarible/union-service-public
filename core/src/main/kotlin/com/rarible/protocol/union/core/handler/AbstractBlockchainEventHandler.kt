package com.rarible.protocol.union.core.handler

import com.rarible.protocol.union.dto.BlockchainDto
import org.slf4j.LoggerFactory

abstract class AbstractBlockchainEventHandler<B, U>(
    override val blockchain: BlockchainDto
) : BlockchainEventHandler<B, U> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: B) {
        try {
            handleSafely(event)
        } catch (ex: Exception) {
            logger.error("Unexpected exception during handling event [{}]", event, ex)
        }
    }

}
