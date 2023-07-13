package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.ActivityEvent
import com.rarible.protocol.union.core.event.OutgoingActivityEventListener
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.enrichment.converter.EnrichmentActivityDtoConverter
import org.springframework.stereotype.Component

@Component
class EnrichmentActivityEventService(
    private val activityEventListeners: List<OutgoingActivityEventListener>,
    private val enrichmentActivityService: EnrichmentActivityService,
    private val ownershipEventService: EnrichmentOwnershipEventService,
    private val itemEventService: EnrichmentItemEventService,
    private val ff: FeatureFlagsProperties,
) {

    suspend fun onActivity(activity: UnionActivity) {
        val marks = activity.eventTimeMarks

        val activityDto = if (ff.enableMongoActivityWrite) {
            val enrichmentActivity = enrichmentActivityService.update(activity)
            EnrichmentActivityDtoConverter.convert(source = enrichmentActivity, reverted = activity.reverted ?: false)
        } else {
            enrichmentActivityService.enrichDeprecated(activity)
        }
        val activityEvent = ActivityEvent(
            activity = activityDto,
            eventTimeMarks = marks?.toDto()
        )

        if (ff.enableItemLastSaleEnrichment) {
            itemEventService.onActivity(
                activity = activity,
                item = null,
                eventTimeMarks = marks
            )
        }

        if (ff.enableOwnershipSourceEnrichment) {
            ownershipEventService.onActivity(
                activity = activity,
                ownership = null,
                eventTimeMarks = marks
            )
        }

        val shouldSend = ff.enableRevertedActivityEventSending || activity.reverted != true
        if (shouldSend) {
            activityEventListeners.onEach { it.onEvent(activityEvent) }
        }
    }

}