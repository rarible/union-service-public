package com.rarible.protocol.union.listener.handler

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.event.OutgoingActivityEventListener
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.dto.ActivityDto
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.EVENT)
class UnionActivityEventHandler(
    private val activityEventListeners: List<OutgoingActivityEventListener>,
    private val ff: FeatureFlagsProperties
) : IncomingEventHandler<ActivityDto> {

    override suspend fun onEvent(event: ActivityDto) {
        if (event.reverted == true) {
            if (!ff.enableRevertedActivityEvents) {
                return
            }
        }
        activityEventListeners.onEach { it.onEvent(event) }
    }
}
