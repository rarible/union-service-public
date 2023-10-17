package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.event.OutgoingEventListener
import com.rarible.protocol.union.core.model.ActivityEvent
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.enrichment.converter.EnrichmentActivityDtoConverter
import org.springframework.stereotype.Component

@Component
class EnrichmentActivityEventService(
    private val activityEventListeners: List<OutgoingEventListener<ActivityEvent>>,
    private val enrichmentActivityService: EnrichmentActivityService,
    private val ownershipEventService: EnrichmentOwnershipEventService,
    private val itemEventService: EnrichmentItemEventService,
    private val ff: FeatureFlagsProperties,
) {

    suspend fun onActivity(activity: UnionActivity) {
        val marks = activity.eventTimeMarks

        val enrichmentActivity = enrichmentActivityService.update(activity)
        val activityDto = EnrichmentActivityDtoConverter.convert(
            source = enrichmentActivity,
            reverted = activity.reverted ?: false
        )

        val activityEvent = ActivityEvent(
            activity = activityDto,
            eventTimeMarks = marks?.toDto()
        )

        itemEventService.onActivity(
            activity = activity,
            item = null,
            eventTimeMarks = marks
        )

        if (ff.enableOwnershipSourceEnrichment) {
            ownershipEventService.onActivity(
                activity = activity,
                ownership = null,
                eventTimeMarks = marks
            )
        }

        activityEventListeners.onEach { it.onEvent(activityEvent) }
    }
}
