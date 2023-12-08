package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.enrichment.event.EnrichmentKafkaEventFactory
import com.rarible.protocol.union.enrichment.model.ItemChangeEvent
import org.springframework.stereotype.Component

@Component
class ItemChangeService(
    private val itemChangeEventProducer: RaribleKafkaProducer<ItemChangeEvent>
) {
    suspend fun onItemChange(change: ItemChangeEvent) {
        itemChangeEventProducer.send(
            EnrichmentKafkaEventFactory.itemChangeEvent(change)
        ).ensureSuccess()
    }
}
