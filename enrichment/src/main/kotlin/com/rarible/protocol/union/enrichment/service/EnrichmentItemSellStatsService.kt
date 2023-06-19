package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.enrichment.model.ItemSellStats
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import org.springframework.stereotype.Service
import java.math.BigInteger

@Service
class EnrichmentItemSellStatsService {

    fun incrementSellStats(
        item: ShortItem,
        oldOwnership: ShortOwnership?,
        newOwnership: ShortOwnership?
    ): ItemSellStats {
        val newStock = newOwnership?.bestSellOrder?.makeStock ?: BigInteger.ZERO
        val oldStock = oldOwnership?.bestSellOrder?.makeStock ?: BigInteger.ZERO
        val stockChange = newStock - oldStock
        val sellersChange = when {
            newStock > BigInteger.ZERO && oldStock == BigInteger.ZERO -> 1
            newStock == BigInteger.ZERO && oldStock > BigInteger.ZERO -> -1
            else -> 0
        }
        return ItemSellStats(sellers = item.sellers + sellersChange, totalStock = item.totalStock + stockChange)
    }
}