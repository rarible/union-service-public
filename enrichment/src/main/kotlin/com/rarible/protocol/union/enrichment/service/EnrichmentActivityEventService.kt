package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.event.OutgoingActivityEventListener
import com.rarible.protocol.union.dto.ActivityDto
import org.springframework.stereotype.Component

@Component
class EnrichmentActivityEventService(
    private val activityEventListeners: List<OutgoingActivityEventListener>,
    private val ownershipEventService: EnrichmentOwnershipEventService,
    private val itemEventService: EnrichmentItemEventService,
    private val ff: FeatureFlagsProperties
) {

    suspend fun onActivity(activity: ActivityDto) {
        if (ff.enableItemLastSaleEnrichment) {
            itemEventService.onActivity(activity)
        }

        if (ff.enableOwnershipSourceEnrichment) {
            ownershipEventService.onActivity(activity)
        }

        val shouldSend = ff.enableRevertedActivityEventSending || activity.reverted != true
        if (shouldSend) {
            activityEventListeners.onEach { it.onEvent(activity) }
        }
    }

}