package com.rarible.protocol.union.core.handler

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.protocol.union.dto.BlockchainDto
import org.slf4j.LoggerFactory

class BlockchainEventHandlerWrapper<B, U>(
    private val blockchainHandler: BlockchainEventHandler<B, U>
) : BlockchainEventHandler<B, U>, ConsumerEventHandler<B> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: B) {
        try {
            blockchainHandler.handle(event)
        } catch (ex: Exception) {
            logger.error("Unexpected exception during handling event [{}], {}", event, ex)

            // TODO: remove
            if (ex is InvalidFormatException) {
                logger.error("Skip invalid format")
            } else {
                throw ex
            }
        }
    }

    override val blockchain: BlockchainDto = blockchainHandler.blockchain

    override val handler: IncomingEventHandler<U> = blockchainHandler.handler

}
