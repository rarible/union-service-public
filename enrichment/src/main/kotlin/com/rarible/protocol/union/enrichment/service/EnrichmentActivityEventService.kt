package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.event.OutgoingActivityEventListener
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.model.blockchainAndIndexerMarks
import com.rarible.protocol.union.core.model.isBlockchainEvent
import com.rarible.protocol.union.core.model.offchainAndIndexerMarks
import com.rarible.protocol.union.dto.ActivityDto
import org.springframework.stereotype.Component

@Component
class EnrichmentActivityEventService(
    private val activityEventListeners: List<OutgoingActivityEventListener>,
    private val enrichmentActivityService: EnrichmentActivityService,
    private val ownershipEventService: EnrichmentOwnershipEventService,
    private val itemEventService: EnrichmentItemEventService,
    private val ff: FeatureFlagsProperties
) {

    @Deprecated("keep UnionActivity only")
    suspend fun onActivity(activity: ActivityDto) {
        // Workaround since we can't pass EVentTimeMarks for activities
        val isBlockchainEvent = activity.isBlockchainEvent()
        val marks = if (isBlockchainEvent) {
            blockchainAndIndexerMarks(activity.date)
        } else {
            offchainAndIndexerMarks(activity.date)
        }.add("enrichment-in")

        if (ff.enableItemLastSaleEnrichment) {
            itemEventService.onActivityLegacy(
                activity = activity,
                item = null,
                eventTimeMarks = marks
            )
        }

        if (ff.enableOwnershipSourceEnrichment) {
            ownershipEventService.onActivityLegacy(
                activity = activity,
                ownership = null,
                eventTimeMarks = marks
            )
        }

        val shouldSend = ff.enableRevertedActivityEventSending || activity.reverted != true
        if (shouldSend) {
            activityEventListeners.onEach { it.onEvent(activity) }
        }
    }

    suspend fun onActivity(activity: UnionActivity) {
        // Workaround since we can't pass EVentTimeMarks for activities
        val isBlockchainEvent = activity.isBlockchainEvent()
        val marks = if (isBlockchainEvent) {
            blockchainAndIndexerMarks(activity.date)
        } else {
            offchainAndIndexerMarks(activity.date)
        }.add("enrichment-in")

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
            val dto = enrichmentActivityService.enrich(activity)
            activityEventListeners.onEach { it.onEvent(dto) }
        }
    }

}