package com.rarible.protocol.union.worker.task

import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomInt
import com.rarible.protocol.union.enrichment.model.ItemSellStats
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class ItemSellStatsTaskHandlerTest {
    @InjectMockKs
    private lateinit var itemSellStatsTaskHandler: ItemSellStatsTaskHandler

    @MockK
    private lateinit var enrichmentOwnershipService: EnrichmentOwnershipService

    @MockK
    private lateinit var itemRepository: ItemRepository

    @Test
    fun run() = runBlocking<Unit> {
        val fromId = ShortItemId(randomEthItemId())
        val shortItem1 = randomShortItem()
        val shortItem2 = randomShortItem()
        coEvery { itemRepository.findAll(fromId) } returns flowOf(shortItem1, shortItem2)
        val shortItem1ForUpdate = randomShortItem()
        val shortItem2ForUpdate = randomShortItem()
        coEvery { itemRepository.get(shortItem1.id) } returns shortItem1ForUpdate
        coEvery { itemRepository.get(shortItem2.id) } returns shortItem2ForUpdate
        coEvery { enrichmentOwnershipService.getItemSellStats(shortItem1.id) } returns ItemSellStats(
            sellers = shortItem1ForUpdate.sellers,
            totalStock = shortItem1ForUpdate.totalStock
        )
        val stats = ItemSellStats(
            sellers = randomInt(),
            totalStock = randomBigInt()
        )
        coEvery { enrichmentOwnershipService.getItemSellStats(shortItem2.id) } returns stats

        coEvery {
            itemRepository.save(
                shortItem2ForUpdate.copy(
                    sellers = stats.sellers,
                    totalStock = stats.totalStock
                )
            )
        } returns shortItem2ForUpdate

        assertThat(
            itemSellStatsTaskHandler.runLongTask(fromId.toString(), "").toList()
        ).containsExactly(shortItem1.id.toString(), shortItem2.id.toString())

        coVerify {
            itemRepository.save(
                shortItem2ForUpdate.copy(
                    sellers = stats.sellers,
                    totalStock = stats.totalStock
                )
            )
        }
    }
}