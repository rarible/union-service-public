package com.rarible.protocol.union.listener.handler

import com.rarible.core.daemon.sequential.ConsumerEventHandler
import org.slf4j.LoggerFactory

abstract class AbstractEventHandler<T> : ConsumerEventHandler<T> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: T) {
        try {
            handleSafely(event)
            // TODO add some specific exception handling
        } catch (ex: Exception) {
            logger.error("Unexpected exception during handling event [{}]", event, ex)
        }
    }

    abstract suspend fun handleSafely(event: T)
}