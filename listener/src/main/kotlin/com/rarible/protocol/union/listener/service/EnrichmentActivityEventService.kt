package com.rarible.protocol.union.listener.service

import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.event.OutgoingActivityEventListener
import com.rarible.protocol.union.dto.ActivityDto
import org.springframework.stereotype.Component

@Component
class EnrichmentActivityEventService(
    private val activityEventListeners: List<OutgoingActivityEventListener>,
    private val ownershipEventService: EnrichmentOwnershipEventService,
    private val ff: FeatureFlagsProperties
) {

    suspend fun onActivity(activity: ActivityDto) {
        val shouldHandle = ff.enableRevertedActivityEventHandling || activity.reverted != true
        if (shouldHandle) {
            ownershipEventService.onActivity(activity)
        }

        val shouldSend = ff.enableRevertedActivityEventSending || activity.reverted != true
        if (shouldSend) {
            activityEventListeners.onEach { it.onEvent(activity) }
        }
    }

}