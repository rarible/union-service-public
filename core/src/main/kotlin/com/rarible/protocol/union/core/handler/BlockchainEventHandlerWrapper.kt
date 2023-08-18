package com.rarible.protocol.union.core.handler

import com.rarible.core.kafka.RaribleKafkaBatchEventHandler
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

class BlockchainEventHandlerWrapper<B, U>(
    private val blockchainHandler: BlockchainEventHandler<B, U>,
    private val eventCounter: AtomicInteger
) : BlockchainEventHandler<B, U>, /*RaribleKafkaEventHandler<B>,*/ RaribleKafkaBatchEventHandler<B> {

    override val eventType = blockchainHandler.eventType
    override val blockchain = blockchainHandler.blockchain
    override val handler = blockchainHandler.handler

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: B) {
        try {
            eventCounter.incrementAndGet()
            blockchainHandler.handle(event)
        } catch (ex: EventConversionException) {
            logger.error("Conversion of single event failed [{}]", ex.event, ex.cause)
            eventCounter.decrementAndGet()
            throw ex.cause!!
        } catch (ex: Exception) {
            logger.error("Unexpected exception during handling event [{}]", event, ex)
            eventCounter.decrementAndGet()
            throw ex
        }
    }

    override suspend fun handle(events: List<B>) {
        try {
            eventCounter.addAndGet(events.size)
            blockchainHandler.handle(events)
        } catch (ex: EventConversionException) {
            logger.error(
                "Conversion of one of ${events.size} $blockchain ${eventType.name} events failed [{}]",
                ex.event, ex.cause
            )
            eventCounter.addAndGet(-events.size)
            throw ex.cause!!
        } catch (ex: Exception) {
            logger.error(
                "Unexpected exception during handling batch of ${events.size} $blockchain ${eventType.name} events", ex
            )
            eventCounter.addAndGet(-events.size)
            throw ex
        }
    }
}
