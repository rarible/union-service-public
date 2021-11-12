package com.rarible.protocol.union.listener.handler

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.union.core.event.OutgoingActivityEventListener
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.dto.ActivityDto
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = "app", subtype = "event")
class UnionActivityEventHandler(
    private val activityEventListeners: List<OutgoingActivityEventListener>
) : IncomingEventHandler<ActivityDto> {

    override suspend fun onEvent(event: ActivityDto) {
        activityEventListeners.onEach { it.onEvent(event) }
    }
}
