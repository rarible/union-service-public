package com.rarible.protocol.union.listener.handler

import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.protocol.union.dto.UnionEventTopicProvider
import org.slf4j.LoggerFactory

abstract class AbstractEventHandler<T> : ConsumerEventHandler<T> {

    companion object {
        val ITEM_EVENT_HEADERS = mapOf("protocol.union.item.event.version" to UnionEventTopicProvider.VERSION)
        val OWNERSHIP_EVENT_HEADERS = mapOf("protocol.union.ownership.event.version" to UnionEventTopicProvider.VERSION)
        val ORDER_EVENT_HEADERS = mapOf("protocol.union.order.event.version" to UnionEventTopicProvider.VERSION)
        val ACTIVITY_EVENT_HEADERS = mapOf("protocol.union.activity.event.version" to UnionEventTopicProvider.VERSION)
    }

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