package com.rarible.protocol.union.listener.handler

import com.rarible.protocol.union.core.event.EventCountMetrics
import com.rarible.protocol.union.core.handler.InternalBatchEventHandler
import com.rarible.protocol.union.core.handler.InternalEventHandler
import com.rarible.protocol.union.core.model.UnionInternalBlockchainEvent
import org.springframework.stereotype.Component

@Component
class MetricsInternalEventHandlerFactory(
    private val eventCountMetrics: EventCountMetrics,
) {
    fun create(handler: InternalEventHandler<UnionInternalBlockchainEvent>) =
        MetricsInternalEventHandler(eventCountMetrics, handler)

    fun create(handler: InternalBatchEventHandler<UnionInternalBlockchainEvent>) =
        MetricsInternalBatchEventHandler(eventCountMetrics, handler)
}

class MetricsInternalEventHandler(
    private val eventCountMetrics: EventCountMetrics,
    private val handler: InternalEventHandler<UnionInternalBlockchainEvent>
) : InternalEventHandler<UnionInternalBlockchainEvent> {
    override suspend fun handle(event: UnionInternalBlockchainEvent) {
        try {
            eventCountMetrics.eventReceived(
                EventCountMetrics.Stage.INTERNAL,
                event.getBlockchain(),
                event.getEventType()
            )
            handler.handle(event)
        } catch (e: Throwable) {
            eventCountMetrics.eventReceived(
                EventCountMetrics.Stage.INTERNAL,
                event.getBlockchain(),
                event.getEventType(),
                -1
            )
            throw e
        }
    }
}

class MetricsInternalBatchEventHandler(
    private val eventCountMetrics: EventCountMetrics,
    private val handler: InternalBatchEventHandler<UnionInternalBlockchainEvent>
) : InternalBatchEventHandler<UnionInternalBlockchainEvent> {
    override suspend fun handle(event: List<UnionInternalBlockchainEvent>) {
        try {
            event.forEach {
                eventCountMetrics.eventReceived(EventCountMetrics.Stage.INTERNAL, it.getBlockchain(), it.getEventType())
            }
            handler.handle(event)
        } catch (e: Throwable) {
            event.forEach {
                eventCountMetrics.eventReceived(
                    EventCountMetrics.Stage.INTERNAL,
                    it.getBlockchain(),
                    it.getEventType(),
                    -1
                )
            }
            throw e
        }
    }
}
