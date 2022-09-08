package com.rarible.protocol.union.listener.handler

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemMetaEvent
import com.rarible.protocol.union.core.model.UnionItemMetaRefreshEvent
import com.rarible.protocol.union.core.model.UnionItemMetaUpdateEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.service.ItemMetaService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.EVENT)
class UnionItemMetaEventHandler(
    private val itemMetaService: ItemMetaService,
    private val enrichmentItemService: EnrichmentItemService,
    private val handler: IncomingEventHandler<UnionItemEvent>
) : IncomingEventHandler<UnionItemMetaEvent> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun onEvent(event: UnionItemMetaEvent) {
        return when (event) {
            is UnionItemMetaRefreshEvent -> {
                logger.info("Refreshing meta for item {} by request of ItemMetaRefreshEvent", event.itemId)
                itemMetaService.schedule(event.itemId, "default", true) // TODO PT-49
            }
            is UnionItemMetaUpdateEvent -> {
                logger.info("Saving meta for item {} by MetaUpdateEvent", event.itemId)
                itemMetaService.save(event.itemId, event.unionMeta)

                val item = event.unionItem ?: enrichmentItemService.fetchOrNull(ShortItemId(event.itemId))
                if (item != null) {
                    logger.info("Sending item {} update event caused by meta update", event.itemId)
                    handler.onEvent(UnionItemUpdateEvent(item))
                } else {
                    logger.info("Item {} is not found to send meta update event", event.itemId)
                }
            }
        }
    }

    override suspend fun onEvents(events: Collection<UnionItemMetaEvent>) {
        events.forEach { onEvent(it) }
    }

}