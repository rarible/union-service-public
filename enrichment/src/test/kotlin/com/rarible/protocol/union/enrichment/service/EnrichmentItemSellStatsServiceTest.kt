package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.dto.OwnershipSourceDto
import com.rarible.protocol.union.enrichment.model.ItemSellStats
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomShortOrder
import com.rarible.protocol.union.enrichment.test.data.randomShortOwnership
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigInteger

@ExtendWith(MockKExtension::class)
internal class EnrichmentItemSellStatsServiceTest {

    @InjectMockKs
    private lateinit var enrichmentItemSellStatsService: EnrichmentItemSellStatsService

    private lateinit var item: ShortItem

    @BeforeEach
    fun before() {
        item = randomShortItem().copy(totalStock = BigInteger.TEN, sellers = 10)
    }

    @Test
    fun `new ownership with best sell order increment stats`() {
        val ownership = randomShortOwnership().copy(
            bestSellOrder = randomShortOrder(makeStock = 2.toBigInteger())
        )
        val result = enrichmentItemSellStatsService.incrementSellStats(
            item = item,
            oldOwnership = null,
            newOwnership = ownership
        )
        assertThat(result).isEqualTo(ItemSellStats(sellers = 11, totalStock = 12.toBigInteger()))
    }

    @Test
    fun `ownership best sell order change`() {
        val oldOwnership = randomShortOwnership().copy(
            bestSellOrder = randomShortOrder(makeStock = 3.toBigInteger())
        )
        val newOwnership = randomShortOwnership().copy(
            bestSellOrder = randomShortOrder(makeStock = 2.toBigInteger())
        )
        val result = enrichmentItemSellStatsService.incrementSellStats(
            item = item,
            oldOwnership = oldOwnership,
            newOwnership = newOwnership
        )
        assertThat(result).isEqualTo(ItemSellStats(sellers = 10, totalStock = 9.toBigInteger()))
    }

    @Test
    fun `new ownership without order`() {
        val newOwnership = randomShortOwnership().copy(
            bestSellOrder = null
        )
        val result = enrichmentItemSellStatsService.incrementSellStats(
            item = item,
            oldOwnership = null,
            newOwnership = newOwnership
        )
        assertThat(result).isEqualTo(ItemSellStats(sellers = 10, totalStock = BigInteger.TEN))
    }

    @Test
    fun `ownership order not changed`() {
        val order = randomShortOrder()
        val oldOwnership = randomShortOwnership().copy(
            bestSellOrder = order,
            source = OwnershipSourceDto.MINT,
        )
        val newOwnership = randomShortOwnership().copy(
            bestSellOrder = order,
            source = OwnershipSourceDto.PURCHASE,
        )
        val result = enrichmentItemSellStatsService.incrementSellStats(
            item = item,
            oldOwnership = oldOwnership,
            newOwnership = newOwnership
        )
        assertThat(result).isEqualTo(ItemSellStats(sellers = 10, totalStock = BigInteger.TEN))
    }

    @Test
    fun `ownership with order deleted`() {
        val oldOwnership = randomShortOwnership().copy(
            bestSellOrder = randomShortOrder(makeStock = 3.toBigInteger())
        )
        val result = enrichmentItemSellStatsService.incrementSellStats(
            item = item,
            oldOwnership = oldOwnership,
            newOwnership = null,
        )
        assertThat(result).isEqualTo(ItemSellStats(sellers = 9, totalStock = 7.toBigInteger()))
    }

    @Test
    fun `ownership without order deleted`() {
        val oldOwnership = randomShortOwnership().copy(
            bestSellOrder = null,
        )
        val result = enrichmentItemSellStatsService.incrementSellStats(
            item = item,
            oldOwnership = oldOwnership,
            newOwnership = null,
        )
        assertThat(result).isEqualTo(ItemSellStats(sellers = 10, totalStock = BigInteger.TEN))
    }

    @Test
    fun `ownership removed from sale`() {
        val oldOwnership = randomShortOwnership().copy(
            bestSellOrder = randomShortOrder(makeStock = 2.toBigInteger())
        )
        val result = enrichmentItemSellStatsService.incrementSellStats(
            item = item,
            oldOwnership = oldOwnership,
            newOwnership = null
        )
        assertThat(result).isEqualTo(ItemSellStats(sellers = 9, totalStock = 8.toBigInteger()))
    }
}
