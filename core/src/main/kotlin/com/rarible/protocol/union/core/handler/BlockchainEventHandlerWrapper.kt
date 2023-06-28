package com.rarible.protocol.union.core.handler

import com.rarible.core.kafka.RaribleKafkaBatchEventHandler
import org.slf4j.LoggerFactory

class BlockchainEventHandlerWrapper<B, U>(
    private val blockchainHandler: BlockchainEventHandler<B, U>
) : BlockchainEventHandler<B, U>, /*RaribleKafkaEventHandler<B>,*/ RaribleKafkaBatchEventHandler<B> {

    override val eventType = blockchainHandler.eventType
    override val blockchain = blockchainHandler.blockchain
    override val handler = blockchainHandler.handler

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: B) {
        try {
            blockchainHandler.handle(event)
        } catch (ex: EventConversionException) {
            logger.error("Conversion of single event failed [{}]", ex.event, ex.cause)
            throw ex.cause!!
        } catch (ex: Exception) {
            logger.error("Unexpected exception during handling event [{}]", event, ex)
            throw ex
        }
    }

    override suspend fun handle(events: List<B>) {
        try {
            blockchainHandler.handle(events)
        } catch (ex: EventConversionException) {
            logger.error(
                "Conversion of one of ${events.size} $blockchain ${eventType.name} events failed [{}]",
                ex.event, ex.cause
            )
            throw ex.cause!!
        } catch (ex: Exception) {
            logger.error(
                "Unexpected exception during handling batch of ${events.size} $blockchain ${eventType.name} events", ex
            )
            throw ex
        }
    }

}
