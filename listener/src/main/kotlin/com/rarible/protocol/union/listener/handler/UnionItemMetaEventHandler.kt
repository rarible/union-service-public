package com.rarible.protocol.union.listener.handler

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemMetaEvent
import com.rarible.protocol.union.core.model.UnionItemMetaRefreshEvent
import com.rarible.protocol.union.core.model.UnionItemMetaUpdateEvent
import com.rarible.protocol.union.enrichment.meta.UnionMetaService
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.EVENT)
class UnionItemMetaEventHandler(
    private val unionMetaService: UnionMetaService
) : IncomingEventHandler<UnionItemMetaEvent> {

    override suspend fun onEvent(event: UnionItemMetaEvent) {
        return when (event) {
            is UnionItemMetaRefreshEvent -> unionMetaService.scheduleLoading(event.itemId)
            is UnionItemMetaUpdateEvent -> unionMetaService.save(event.itemId, event.unionMeta)
        }
    }
}