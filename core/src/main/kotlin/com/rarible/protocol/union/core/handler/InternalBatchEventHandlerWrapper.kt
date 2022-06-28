package com.rarible.protocol.union.core.handler

import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import org.slf4j.LoggerFactory

class InternalBatchEventHandlerWrapper<B>(
    private val internalHandler: InternalBatchEventHandler<B>
) : InternalBatchEventHandler<B>, ConsumerBatchEventHandler<B> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(events: List<B>) {
        try {
            internalHandler.handle(events)
        } catch (ex: Exception) {
            logger.error("Unexpected exception during handling internal event batch", ex)
        }
    }

}
