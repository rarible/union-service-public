package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.service.ReconciliationEventService
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.enrichment.service.EnrichmentActivityEventService
import org.slf4j.LoggerFactory
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

    @CaptureTransaction("UnionActivityEvent")
    suspend fun onEvent(event: UnionActivity) {
        if (!event.isValid()) {
            logger.info("Ignoring activity event $event as it is not valid")
            return
        }
        try {
            enrichmentActivityEventService.onActivity(event)
        } catch (e: Throwable) {
            reconciliationEventService.onFailedActivity(event)
            throw e
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UnionInternalActivityEventHandler::class.java)
    }
}