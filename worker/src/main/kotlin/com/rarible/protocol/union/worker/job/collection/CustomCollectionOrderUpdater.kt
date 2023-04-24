package com.rarible.protocol.union.worker.job.collection

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OrderUpdateEventDto
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CustomCollectionOrderUpdater(
    // TODO ideally, we should take this data from mongo instead of indexer'sAPI
    private val router: BlockchainRouter<OrderService>,
    private val enrichmentOrderService: EnrichmentOrderService,
    private val eventProducer: RaribleKafkaProducer<OrderEventDto>
) : CustomCollectionUpdater {

    private val batchSize = 200

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun update(item: UnionItem) = coroutineScope<Unit> {
        val itemId = item.id
        val service = router.getService(itemId.blockchain)

        val sell = service.getSellCurrencies(itemId.value)
            .map { async { updateSell(itemId, it.currencyId()!!) } }

        val bid = service.getBidCurrencies(itemId.value)
            .map { async { updateBid(itemId, it.currencyId()!!) } }

        sell.awaitAll()
        bid.awaitAll()
    }

    private suspend fun updateSell(itemId: ItemIdDto, currencyId: String) {
        val service = router.getService(itemId.blockchain)
        var continuation: String? = null
        do {
            val page = service.getSellOrdersByItem(
                platform = null,
                itemId = itemId.value,
                continuation = continuation,
                maker = null,
                origin = null,
                status = emptyList(),
                currencyId = currencyId,
                size = batchSize
            )

            send(page.entities, itemId, currencyId, "SELL")

            continuation = page.continuation
        } while (continuation != null)
    }

    private suspend fun updateBid(itemId: ItemIdDto, currencyId: String) {
        val service = router.getService(itemId.blockchain)
        var continuation: String? = null
        do {
            val page = service.getOrderBidsByItem(
                platform = null,
                itemId = itemId.value,
                continuation = continuation,
                origin = null,
                status = emptyList(),
                makers = null,
                start = null,
                end = null,
                currencyAddress = currencyId,
                size = batchSize
            )

            send(page.entities, itemId, currencyId, "BID")

            continuation = page.continuation
        } while (continuation != null)
    }

    private suspend fun send(
        orders: List<UnionOrder>,
        itemId: ItemIdDto,
        currencyId: String,
        type: String
    ) {
        if (orders.isEmpty()) {
            return
        }
        val messages = enrichmentOrderService.enrich(orders).map {
            KafkaEventFactory.orderEvent(
                OrderUpdateEventDto(
                    eventId = UUID.randomUUID().toString(),
                    orderId = it.id,
                    order = it,
                    eventTimeMarks = null // We don't need it here
                )
            )
        }
        eventProducer.send(messages)
        logger.info(
            "Updated {} {} orders for currency {} for custom collection migration of Item {}",
            messages.size, type, currencyId, itemId.fullId()
        )
    }

}