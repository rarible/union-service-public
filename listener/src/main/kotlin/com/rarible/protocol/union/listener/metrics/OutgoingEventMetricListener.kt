package com.rarible.protocol.union.listener.metrics

import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.event.OutgoingEventListener
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EventTimeMarksDto

abstract class OutgoingEventMetricListener<T> : OutgoingEventListener<T> {

    fun onEvent(
        blockchainDto: BlockchainDto,
        eventType: EventType,
        eventTimeMarks: EventTimeMarksDto?
    ) {
        // TODO add implementation
    }

}

