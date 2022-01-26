package com.rarible.protocol.union.core.service

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.ext
import com.rarible.protocol.union.enrichment.model.ReconciliationMarkEvent
import org.springframework.stereotype.Component

@Component
class ReconciliationEventService(
    private val eventsProducer: RaribleKafkaProducer<ReconciliationMarkEvent>
) {

    suspend fun onCorruptedItem(itemId: ItemIdDto) {
        eventsProducer.send(
            KafkaEventFactory.reconciliationItemMarkEvent(itemId)
        ).ensureSuccess()
    }

    suspend fun onCorruptedOwnership(ownershipId: OwnershipIdDto) {
        eventsProducer.send(
            KafkaEventFactory.reconciliationOwnershipMarkEvent(ownershipId)
        ).ensureSuccess()
    }

    suspend fun onFailedOrder(order: OrderDto) {

        val blockchain = order.id.blockchain
        val makeAssetExt = order.make.type.ext
        val takeAssetExt = order.take.type.ext

        val makeItemId = makeAssetExt.itemId
        val takeItemId = takeAssetExt.itemId

        if (makeItemId != null) {
            val ownershipId = OwnershipIdDto(
                makeItemId.blockchain,
                makeItemId.contract,
                makeItemId.tokenId,
                UnionAddressConverter.convert(blockchain, order.maker.value)
            )
            onCorruptedItem(makeItemId)
            onCorruptedOwnership(ownershipId)
        }

        if (takeItemId != null) {
            onCorruptedItem(takeItemId)
        }
    }

}