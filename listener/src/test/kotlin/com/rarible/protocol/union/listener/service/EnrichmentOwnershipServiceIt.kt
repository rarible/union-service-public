package com.rarible.protocol.union.listener.service

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.enrichment.test.data.randomShortOwnership
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrder
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class EnrichmentOwnershipServiceIt {

    @Autowired
    private lateinit var ownershipService: EnrichmentOwnershipService

    @Test
    fun getTotalStock() = runBlocking<Unit> {
        val itemId = randomEthItemId()

        val orderDto1 = ShortOrderConverter.convert(randomUnionSellOrder(itemId).copy(makeStock = 54.toBigDecimal()))
        val orderDto2 = ShortOrderConverter.convert(randomUnionSellOrder(itemId).copy(makeStock = 33.toBigDecimal()))
        val orderDto3 = ShortOrderConverter.convert(randomUnionSellOrder(itemId).copy(makeStock = 13.toBigDecimal()))

        val ownership1 = randomShortOwnership(itemId).copy(bestSellOrder = orderDto1)
        val ownership2 = randomShortOwnership(itemId).copy(bestSellOrder = orderDto2)
        val ownership3 = randomShortOwnership(itemId).copy(bestSellOrder = orderDto3)

        // should not be included into calculation
        val ownership4 = randomShortOwnership(itemId)
        // Item ID is different
        val ownership5 = randomShortOwnership(randomEthItemId())
            .copy(bestSellOrder = orderDto1)
        // Blockchain is different
        val ownership6 = randomShortOwnership(itemId.copy(blockchain = BlockchainDto.POLYGON))
            .copy(bestSellOrder = orderDto3)

        ownershipService.save(ownership1)
        ownershipService.save(ownership2)
        ownershipService.save(ownership3)
        ownershipService.save(ownership4)
        ownershipService.save(ownership5)
        ownershipService.save(ownership6)

        val itemSellStats = ownershipService.getItemSellStats(ShortItemId(itemId))

        assertEquals(100, itemSellStats.totalStock.toInt())
        assertEquals(3, itemSellStats.sellers)
    }
}