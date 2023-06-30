package com.rarible.protocol.union.listener.handler.internal

import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.service.ReconciliationEventService
import com.rarible.protocol.union.enrichment.service.EnrichmentActivityEventService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class UnionInternalActivityEventHandler(
    private val enrichmentActivityEventService: EnrichmentActivityEventService,
    private val reconciliationEventService: ReconciliationEventService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

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

}