package com.rarible.protocol.union.listener.job

import com.rarible.protocol.union.core.event.OutgoingItemEventListener
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.converter.EnrichedItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.test.data.randomUnionBidOrderDto
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.protocol.union.listener.test.data.defaultUnionListenerProperties
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class OpenSeaOrderItemCleanupJobIt {

    @Autowired
    lateinit var itemRepository: ItemRepository

    val itemService: EnrichmentItemService = mockk()
    val listener: OutgoingItemEventListener = mockk()
    val filter: OpenSeaCleanupOrderFilter = mockk()

    lateinit var job: OpenSeaOrderItemCleanupJob

    @BeforeEach
    fun beforeEach() {
        job = OpenSeaOrderItemCleanupJob(
            itemRepository, itemService, listOf(listener), filter, defaultUnionListenerProperties()
        )
        coEvery { listener.onEvent(any()) } returns Unit
        coEvery { filter.isOld(any(), any(), any()) } returns false
        coEvery { itemService.enrichItem(shortItem = any(), metaPipeline = any()) } answers {
            val shortItem = it.invocation.args[0] as ShortItem
            EnrichedItemConverter.convert(randomUnionItem(shortItem.id.toDto()), shortItem)
        }

    }

    @Test
    fun `cleanup openSea best sells`() = runBlocking<Unit> {
        val bestSellOs = ShortOrderConverter.convert(randomUnionSellOrderDto().copy(platform = PlatformDto.OPEN_SEA))
        val bestBidOs = ShortOrderConverter.convert(randomUnionBidOrderDto())
        val withOpenSea = ShortItemConverter.convert(randomUnionItem(randomEthItemId())).copy(
            bestSellOrder = bestSellOs,
            bestBidOrder = bestBidOs,
            bestSellOrders = mapOf("123" to bestSellOs),
            bestBidOrders = mapOf("321" to bestBidOs)

        )

        val bestSell = ShortOrderConverter.convert(randomUnionSellOrderDto())
        val bestBid = ShortOrderConverter.convert(randomUnionBidOrderDto())
        val withoutOpenSea = ShortItemConverter.convert(randomUnionItem(randomEthItemId())).copy(
            bestSellOrder = bestSell,
            bestBidOrder = bestBid,
            bestSellOrders = mapOf("123" to bestSell),
            bestBidOrders = mapOf("321" to bestBid)
        )

        val bestSellOsLast = ShortOrderConverter.convert(
            randomUnionSellOrderDto().copy(platform = PlatformDto.OPEN_SEA)
        )
        val withOpenSeaEmpty = ShortItemConverter.convert(randomUnionItem(randomEthItemId())).copy(
            bestSellOrder = bestSellOsLast,
            bestSellOrders = mapOf("123" to bestSellOsLast)
        )

        itemRepository.save(withOpenSea)
        itemRepository.save(withoutOpenSea)
        itemRepository.save(withOpenSeaEmpty)

        job.execute(null).collect()

        val updatedOpenSea = itemRepository.get(withOpenSea.id)!!

        assertThat(updatedOpenSea.bestSellOrder).isNull()
        assertThat(updatedOpenSea.bestSellOrders).isEmpty()

        val skipped = itemRepository.get(withoutOpenSea.id)!!

        assertThat(skipped.copy(version = null)).isEqualTo(withoutOpenSea)

        val deleted = itemRepository.get(withOpenSeaEmpty.id)
        assertThat(deleted).isNull()
    }

}