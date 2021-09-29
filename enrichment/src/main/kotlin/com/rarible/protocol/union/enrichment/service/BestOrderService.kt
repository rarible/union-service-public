package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.enrichment.evaluator.BestBidOrderComparator
import com.rarible.protocol.union.enrichment.evaluator.BestOrderEvaluator
import com.rarible.protocol.union.enrichment.evaluator.BestSellOrderComparator
import com.rarible.protocol.union.enrichment.evaluator.ItemBestBidOrderProvider
import com.rarible.protocol.union.enrichment.evaluator.ItemBestSellOrderProvider
import com.rarible.protocol.union.enrichment.evaluator.OwnershipBestSellOrderProvider
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortOrder
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import org.springframework.stereotype.Component

@Component
class BestOrderService(
    private val orderService: OrderService
) {

    // TODO we can return here Full Order if it was fetched - thats allow us to avoid one more query to indexer
    // for update events in ownership/item
    suspend fun getBestSellOrder(ownership: ShortOwnership, order: OrderDto): ShortOrder? {
        val bestOrderEvaluator = BestOrderEvaluator(
            comparator = BestSellOrderComparator,
            provider = OwnershipBestSellOrderProvider(ownership.id, orderService)
        )
        return bestOrderEvaluator.evaluateBestOrder(ownership.bestSellOrder, order)
    }

    suspend fun getBestSellOrder(item: ShortItem, order: OrderDto): ShortOrder? {
        val bestOrderEvaluator = BestOrderEvaluator(
            comparator = BestSellOrderComparator,
            provider = ItemBestSellOrderProvider(item.id, orderService)
        )
        return bestOrderEvaluator.evaluateBestOrder(item.bestSellOrder, order)
    }

    suspend fun getBestBidOrder(item: ShortItem, order: OrderDto): ShortOrder? {
        val bestOrderEvaluator = BestOrderEvaluator(
            comparator = BestBidOrderComparator,
            provider = ItemBestBidOrderProvider(item.id, orderService)
        )
        return bestOrderEvaluator.evaluateBestOrder(item.bestBidOrder, order)
    }

}