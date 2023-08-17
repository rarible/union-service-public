package com.rarible.protocol.union.core.event

import com.rarible.protocol.union.dto.BlockchainDto

class OutgoingEventListenerWrapper<T>(
    private val eventCountMetrics: EventCountMetrics,
    private val delegate: OutgoingEventListener<T>,
    private val eventType: EventType,
    private val eventBlockchain: (T) -> BlockchainDto
) : OutgoingEventListener<T> {

    override suspend fun onEvent(event: T) {
        try {
            eventCountMetrics.eventSent(EventCountMetrics.Stage.EXTERNAL, eventBlockchain(event), eventType)
            delegate.onEvent(event)
        } catch (e: Throwable) {
            eventCountMetrics.eventSent(EventCountMetrics.Stage.EXTERNAL, eventBlockchain(event), eventType, -1)
            throw e
        }
    }
}
