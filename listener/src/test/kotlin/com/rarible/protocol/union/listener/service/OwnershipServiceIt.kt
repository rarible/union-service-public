package com.rarible.protocol.union.listener.service

import com.rarible.core.test.data.randomBigInt
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.enrichment.test.data.randomShortOwnership
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.protocol.union.test.data.randomAddressString
import com.rarible.protocol.union.test.data.randomEthItemId
import com.rarible.protocol.union.test.data.randomUnionOrderDto
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class OwnershipServiceIt {

    @Autowired
    private lateinit var ownershipService: EnrichmentOwnershipService

    @Test
    fun getTotalStock() = runBlocking<Unit> {
        val itemId = randomEthItemId()

        val orderDto1 = ShortOrderConverter.convert(randomUnionOrderDto(itemId).copy(makeStock = 54.toBigDecimal()))
        val orderDto2 = ShortOrderConverter.convert(randomUnionOrderDto(itemId).copy(makeStock = 33.toBigDecimal()))
        val orderDto3 = ShortOrderConverter.convert(randomUnionOrderDto(itemId).copy(makeStock = 13.toBigDecimal()))

        val ownership1 = randomShortOwnership(itemId).copy(bestSellOrder = orderDto1)
        val ownership2 = randomShortOwnership(itemId).copy(bestSellOrder = orderDto2)
        val ownership3 = randomShortOwnership(itemId).copy(bestSellOrder = orderDto3)

        // should not be included into calculation
        val ownership4 = randomShortOwnership(itemId)
        // Token ID is different
        val ownership5 = randomShortOwnership(itemId.copy(tokenId = randomBigInt()))
            .copy(bestSellOrder = orderDto1)
        // Token is different
        val ownership6 = randomShortOwnership(itemId.copy(token = itemId.token.copy(value = randomAddressString())))
            .copy(bestSellOrder = orderDto2)
        // Blockchain is different
        val ownership7 = randomShortOwnership(itemId.copy(blockchain = BlockchainDto.POLYGON))
            .copy(bestSellOrder = orderDto3)

        ownershipService.save(ownership1)
        ownershipService.save(ownership2)
        ownershipService.save(ownership3)
        ownershipService.save(ownership4)
        ownershipService.save(ownership5)
        ownershipService.save(ownership6)
        ownershipService.save(ownership7)

        val itemSellStats = ownershipService.getItemSellStats(ShortItemId(itemId))

        assertEquals(100, itemSellStats.totalStock.toInt())
        assertEquals(3, itemSellStats.sellers)
    }
}