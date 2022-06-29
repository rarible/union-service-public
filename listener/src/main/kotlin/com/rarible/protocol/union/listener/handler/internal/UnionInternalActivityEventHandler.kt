package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.union.core.service.ReconciliationEventService
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.listener.service.EnrichmentActivityEventService
import org.springframework.stereotype.Component

@Component
class UnionInternalActivityEventHandler(
    private val enrichmentActivityEventService: EnrichmentActivityEventService,
    private val reconciliationEventService: ReconciliationEventService
) {

    @CaptureTransaction("UnionActivityEvent")
    suspend fun onEvent(event: ActivityDto) {
        try {
            enrichmentActivityEventService.onActivity(event)
        } catch (e: Throwable) {
            reconciliationEventService.onFailedActivity(event)
            throw e
        }
    }
}