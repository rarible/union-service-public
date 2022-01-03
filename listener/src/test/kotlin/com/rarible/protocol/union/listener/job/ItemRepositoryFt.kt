package com.rarible.protocol.union.listener.job

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrderDto
import com.rarible.protocol.union.integration.ethereum.data.*
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@FlowPreview
@IntegrationTest
internal class ItemRepositoryFt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @BeforeEach
    fun beforeEach() = runBlocking<Unit> {
        itemRepository.createIndices()
    }

    @Test
    fun `should find all items with target platform`() = runBlocking<Unit> {
        val sellOrder1 = ShortOrderConverter.convert(
            randomUnionSellOrderDto(randomEthItemId()).copy(platform = PlatformDto.OPEN_SEA)
        )
        val item1 = randomShortItem().copy(bestSellOrder = sellOrder1)

        val sellOrder2 = ShortOrderConverter.convert(
            randomUnionSellOrderDto(randomEthItemId()).copy(platform = PlatformDto.RARIBLE)
        )
        val item2 = randomShortItem().copy(bestSellOrder = sellOrder2)

        val sellOrder3 = ShortOrderConverter.convert(
            randomUnionSellOrderDto(randomEthItemId()).copy(platform = PlatformDto.OPEN_SEA)
        )
        val item3 = randomShortItem().copy(bestSellOrder = sellOrder3)

        val sellOrder4 = ShortOrderConverter.convert(
            randomUnionSellOrderDto(randomEthItemId()).copy(platform = PlatformDto.RARIBLE)
        )
        val item4 = randomShortItem().copy(bestSellOrder = sellOrder4)

        val sellOrder5 = ShortOrderConverter.convert(
            randomUnionSellOrderDto(randomEthItemId()).copy(platform = PlatformDto.OPEN_SEA)
        )
        val item5 = randomShortItem().copy(bestSellOrder = sellOrder5)

        val sellOrder6 = ShortOrderConverter.convert(
            randomUnionSellOrderDto(randomEthItemId()).copy(platform = PlatformDto.OPEN_SEA)
        )
        val item6 = randomShortItem().copy(bestSellOrder = sellOrder6)

        listOf(item1, item2, item3, item4, item5, item6).forEach {
            itemRepository.save(it)
        }
        Wait.waitAssert {
            val ollOenSeaItems = itemRepository.findWithSellAndPlatform(PlatformDto.OPEN_SEA.name, null).toList()
            assertThat(ollOenSeaItems.size).isEqualTo(4)
            assertThat(ollOenSeaItems.map { it.id }).containsExactlyInAnyOrder(item1.id, item3.id, item5.id, item6.id)

            val fromOpenSeaItems = itemRepository.findWithSellAndPlatform(PlatformDto.OPEN_SEA.name, ollOenSeaItems[1].id).toList()
            assertThat(fromOpenSeaItems.size).isEqualTo(2)
            assertThat(fromOpenSeaItems.map { it.id }).contains(ollOenSeaItems[2].id, ollOenSeaItems[3].id)
        }
    }
}