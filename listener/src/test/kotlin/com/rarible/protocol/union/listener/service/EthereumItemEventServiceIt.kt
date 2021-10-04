package com.rarible.protocol.union.listener.service

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.union.core.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.core.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.converter.EnrichedItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.protocol.union.test.data.randomEthItemId
import com.rarible.protocol.union.test.data.randomEthLegacyOrderDto
import com.rarible.protocol.union.test.data.randomEthNftItemDto
import com.rarible.protocol.union.test.data.randomUnionItem
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono

@FlowPreview
@IntegrationTest
class EthereumItemEventServiceIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var itemEventService: EnrichmentItemEventService

    @Autowired
    private lateinit var itemService: EnrichmentItemService

    @Test
    fun `update event - item doesn't exist`() = runWithKafka {
        val itemId = randomEthItemId()
        val itemDto = randomUnionItem(itemId)

        val expected = EnrichedItemConverter.convert(itemDto)

        itemEventService.onItemUpdated(itemDto)

        val created = itemService.get(ShortItemId(itemId))

        // Item should not be updated since it wasn't in DB before update
        assertThat(created).isNull()
        // But there should be single Item event "as is"
        Wait.waitAssert {
            val messages = findItemUpdates(itemId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].key).isEqualTo(itemId.fullId())
            assertThat(messages[0].id).isEqualTo(itemId.fullId())
            assertThat(messages[0].value.itemId).isEqualTo(itemId)
            assertThat(messages[0].value.item).isEqualTo(expected)
        }
    }

    @Test
    fun `update event - existing item updated`() = runWithKafka {
        val itemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(itemId)
        val bestSellOrder = randomEthLegacyOrderDto(itemId)
        val bestBidOrder = randomEthLegacyOrderDto(itemId)
        val unionBestSell = EthOrderConverter.convert(bestSellOrder, BlockchainDto.ETHEREUM)
        val unionBestBid = EthOrderConverter.convert(bestBidOrder, BlockchainDto.ETHEREUM)

        val unionItem = EthItemConverter.convert(ethItem, BlockchainDto.ETHEREUM)
        val shortItem = ShortItemConverter.convert(unionItem).copy(
            bestSellOrder = ShortOrderConverter.convert(unionBestSell),
            bestBidOrder = ShortOrderConverter.convert(unionBestBid)
        )

        val expected = EnrichedItemConverter.convert(unionItem)
            .copy(bestSellOrder = unionBestSell, bestBidOrder = unionBestBid)

        itemService.save(shortItem)

        coEvery { testEthereumOrderApi.getOrderByHash(unionBestSell.id.value) } returns bestSellOrder.toMono()
        coEvery { testEthereumOrderApi.getOrderByHash(unionBestBid.id.value) } returns bestBidOrder.toMono()
        coEvery { testEthereumItemApi.getNftItemById(itemId.value) } returns ethItem.toMono()

        itemEventService.onItemUpdated(unionItem)

        val stored = itemService.get(shortItem.id)!!

        assertThat(stored.bestSellOrder).isEqualTo(shortItem.bestSellOrder)
        assertThat(stored.bestBidOrder).isEqualTo(shortItem.bestBidOrder)
        Wait.waitAssert {
            val messages = findItemUpdates(itemId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].key).isEqualTo(itemId.fullId())
            assertThat(messages[0].id).isEqualTo(itemId.fullId())
            assertThat(messages[0].value.itemId).isEqualTo(itemId)
            assertThat(messages[0].value.item).isEqualTo(expected)
        }
    }

    @Test
    fun `delete event - existing item deleted`() = runWithKafka {
        val item = itemService.save(randomShortItem())
        val itemId = item.id.toDto()
        assertThat(itemService.get(item.id)).isNotNull()

        itemEventService.onItemDeleted(itemId)

        assertThat(itemService.get(item.id)).isNull()
        Wait.waitAssert {
            val messages = findItemDeletions(itemId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].key).isEqualTo(itemId.fullId())
            assertThat(messages[0].id).isEqualTo(itemId.fullId())
            assertThat(messages[0].value.itemId).isEqualTo(itemId)
        }
    }

    @Test
    fun `delete event - item doesn't exist`() = runWithKafka {
        val shortItemId = randomShortItem().id
        val itemId = shortItemId.toDto()

        itemEventService.onItemDeleted(itemId)

        assertThat(itemService.get(shortItemId)).isNull()
        Wait.waitAssert {
            val messages = findItemDeletions(itemId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].key).isEqualTo(itemId.fullId())
            assertThat(messages[0].id).isEqualTo(itemId.fullId())
            assertThat(messages[0].value.itemId).isEqualTo(itemId)
        }
    }
}