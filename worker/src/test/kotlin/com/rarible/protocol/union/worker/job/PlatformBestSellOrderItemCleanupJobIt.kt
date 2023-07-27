package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.core.event.OutgoingItemEventListener
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.converter.ItemDtoConverter
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.test.data.randomUnionBidOrder
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrder
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.worker.IntegrationTest
import com.rarible.protocol.union.worker.config.PlatformBestSellCleanUpProperties
import com.rarible.protocol.union.worker.config.WorkerProperties
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class PlatformBestSellOrderItemCleanupJobIt {

    @Autowired
    lateinit var itemRepository: ItemRepository

    val itemService: EnrichmentItemService = mockk()
    val listener: OutgoingItemEventListener = mockk()
    val properties = mockk<WorkerProperties>() {
        every { platformBestSellCleanup } returns PlatformBestSellCleanUpProperties(enabled = true)
    }

    lateinit var job: PlatformBestSellOrderItemCleanupJob

    @BeforeEach
    fun beforeEach() = runBlocking<Unit> {
        itemRepository.createIndices()
        job = PlatformBestSellOrderItemCleanupJob(
            itemRepository, itemService, listOf(listener), properties
        )
        coEvery { listener.onEvent(any()) } returns Unit
        coEvery { itemService.enrichItem(shortItem = any(), metaPipeline = any()) } answers {
            val shortItem = it.invocation.args[0] as ShortItem
            ItemDtoConverter.convert(randomUnionItem(shortItem.id.toDto()), shortItem)
        }
    }

    @Test
    fun `cleanup openSea best sells`() = runBlocking<Unit> {
        val bestSellOs = ShortOrderConverter.convert(randomUnionSellOrder().copy(platform = PlatformDto.OPEN_SEA))
        val bestBidOs = ShortOrderConverter.convert(randomUnionBidOrder())
        val withOpenSea = ShortItemConverter.convert(randomUnionItem(randomEthItemId())).copy(
            bestSellOrder = bestSellOs,
            bestBidOrder = bestBidOs,
            bestSellOrders = mapOf("123" to bestSellOs),
            bestBidOrders = mapOf("321" to bestBidOs)

        )

        val bestSell = ShortOrderConverter.convert(randomUnionSellOrder())
        val bestBid = ShortOrderConverter.convert(randomUnionBidOrder())
        val withoutOpenSea = ShortItemConverter.convert(randomUnionItem(randomEthItemId())).copy(
            bestSellOrder = bestSell,
            bestBidOrder = bestBid,
            bestSellOrders = mapOf("123" to bestSell),
            bestBidOrders = mapOf("321" to bestBid)
        )

        val bestSellOsLast = ShortOrderConverter.convert(
            randomUnionSellOrder().copy(platform = PlatformDto.OPEN_SEA)
        )
        val withOpenSeaEmpty = ShortItemConverter.convert(randomUnionItem(randomEthItemId())).copy(
            bestSellOrder = bestSellOsLast,
            bestSellOrders = mapOf("123" to bestSellOsLast)
        )

        itemRepository.save(withOpenSea)
        itemRepository.save(withoutOpenSea)
        itemRepository.save(withOpenSeaEmpty)

        job.execute(PlatformDto.OPEN_SEA, null).collect()

        val updatedOpenSea = itemRepository.get(withOpenSea.id)!!

        assertThat(updatedOpenSea.bestSellOrder).isNull()
        assertThat(updatedOpenSea.bestSellOrders).isEmpty()

        val skipped = itemRepository.get(withoutOpenSea.id)!!

        assertThat(skipped.copy(version = null)).isEqualTo(withoutOpenSea)

        val updated = itemRepository.get(withOpenSeaEmpty.id)!!
        assertThat(updated).isEqualTo(
            withOpenSeaEmpty.copy(
                bestSellOrder = null,
                bestSellOrders = emptyMap(),
                lastUpdatedAt = updated.lastUpdatedAt,
                version = 1
            )
        )
    }
}
