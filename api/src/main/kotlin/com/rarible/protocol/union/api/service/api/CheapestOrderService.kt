package com.rarible.protocol.union.api.service.api

import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.service.BestOrderService
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderService
import org.springframework.stereotype.Service

@Service
class CheapestOrderService(
    private val esItemRepository: EsItemRepository,
    private val orderRouter: BlockchainRouter<OrderService>,
    private val bestOrderService: BestOrderService,
    private val itemRepository: ItemRepository,
    private val enrichmentOrderService: EnrichmentOrderService,
) {
    suspend fun getCheapestOrder(collectionId: CollectionIdDto): OrderDto? {
        val esItems = esItemRepository.getCheapestItems(collectionId = collectionId.fullId())
        if (esItems.isEmpty()) {
            return null
        }
        val shortItemsByIds =
            itemRepository.getAll(esItems.map { ShortItemId.of(it.itemId) }).associateBy { it.id.toString() }
        if (shortItemsByIds.isEmpty()) {
            return null
        }
        val ordersByCurrency = esItems.filter { shortItemsByIds[it.itemId] != null }
            .associateBy({ it.bestSellCurrency!! }, { shortItemsByIds[it.itemId]!!.bestSellOrder!! })
        val bestOrder = bestOrderService.getBestBidOrderInUsd(ordersByCurrency) ?: return null
        val unionOrder = orderRouter.getService(bestOrder.blockchain).getOrderById(bestOrder.id)
        return enrichmentOrderService.enrich(unionOrder)
    }
}
