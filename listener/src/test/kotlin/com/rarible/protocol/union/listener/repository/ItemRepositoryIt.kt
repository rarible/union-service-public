package com.rarible.protocol.union.listener.repository

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOrder
import com.rarible.protocol.union.enrichment.model.ShortPoolOrder
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomAddressString
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

@IntegrationTest
internal class ItemRepositoryIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @BeforeEach
    fun beforeEach() = runBlocking<Unit> {
        itemRepository.createIndices()
    }

    @Test
    fun `should find all items with target platform`() = runBlocking<Unit> {
        val item1 = itemRepository.save(randomShortItem().copy(bestSellOrder = randomSellOrder(PlatformDto.OPEN_SEA)))
        val item2 = itemRepository.save(randomShortItem().copy(bestSellOrder = randomSellOrder(PlatformDto.RARIBLE)))
        val item3 = itemRepository.save(randomShortItem().copy(bestSellOrder = randomSellOrder(PlatformDto.OPEN_SEA)))
        val item4 = itemRepository.save(randomShortItem().copy(bestSellOrder = randomSellOrder(PlatformDto.RARIBLE)))
        val item5 = itemRepository.save(randomShortItem().copy(bestSellOrder = randomSellOrder(PlatformDto.OPEN_SEA)))
        val item6 = itemRepository.save(randomShortItem().copy(bestSellOrder = randomSellOrder(PlatformDto.OPEN_SEA)))
        val item7 = itemRepository.save(randomShortItem().copy(bestSellOrder = null))

        val openSeaItems = itemRepository.findByPlatformWithSell(PlatformDto.OPEN_SEA, null, null).toList()
        assertThat(openSeaItems.map { it.id }).containsExactlyInAnyOrder(item1.id, item3.id, item5.id, item6.id)

        val fromOpenSeaItems = itemRepository.findByPlatformWithSell(PlatformDto.OPEN_SEA, openSeaItems[1].id, null)
            .toList()
        assertThat(fromOpenSeaItems.size).isEqualTo(2)
        assertThat(fromOpenSeaItems.map { it.id }).contains(openSeaItems[2].id, openSeaItems[3].id)
    }

    @Test
    fun `find all by blockchain`() = runBlocking<Unit> {
        val item1 = itemRepository.save(randomShortItem().copy(blockchain = BlockchainDto.ETHEREUM, itemId = "aaa:1"))
        val item2 = itemRepository.save(randomShortItem().copy(blockchain = BlockchainDto.ETHEREUM, itemId = "bbb:2"))
        val item3 = itemRepository.save(randomShortItem().copy(blockchain = BlockchainDto.ETHEREUM, itemId = "ccc:3"))
        itemRepository.save(randomShortItem().copy(blockchain = BlockchainDto.FLOW))

        val firstPage = itemRepository.findByBlockchain(null, BlockchainDto.ETHEREUM, 2).toList()
        assertThat(firstPage).isEqualTo(listOf(item1, item2))

        val withOffset = itemRepository.findByBlockchain(item1.id, BlockchainDto.ETHEREUM, 3).toList()
        assertThat(withOffset).isEqualTo(listOf(item2, item3))
    }

    private fun randomSellOrder(platform: PlatformDto): ShortOrder {
        val randomSellOrder = randomUnionSellOrderDto(randomEthItemId())
            .copy(platform = platform)

        return ShortOrderConverter.convert(randomSellOrder)
    }

    @Test
    fun `find items from the pool`() = runBlocking<Unit> {
        val poolOrder1 = ShortPoolOrder(randomAddressString(), randomSellOrder(PlatformDto.SUDOSWAP))
        val poolOrder2 = ShortPoolOrder(randomAddressString(), randomSellOrder(PlatformDto.SUDOSWAP))
        val item1 = itemRepository.save(randomShortItem().copy(poolSellOrders = listOf(poolOrder1, poolOrder2)))
        val item2 = itemRepository.save(randomShortItem().copy(poolSellOrders = listOf(poolOrder1)))
        val item3 = itemRepository.save(randomShortItem().copy(poolSellOrders = listOf()))

        val pool1 = itemRepository.findByPoolOrder(item1.blockchain, poolOrder1.order.id).toList()
        assertThat(pool1).containsExactlyInAnyOrder(item1.id, item2.id)

        val pool2 = itemRepository.findByPoolOrder(item1.blockchain, poolOrder2.order.id).toList()
        assertThat(pool2).containsExactlyInAnyOrder(item1.id)
    }

    @Test
    fun `update lastUpdatedAt`() = runBlocking<Unit> {
        val item = itemRepository.save(randomShortItem().copy(lastUpdatedAt = Instant.ofEpochMilli(0)))

        val result = itemRepository.updateLastUpdatedAt(item.id)
        val updated = itemRepository.get(item.id)!!
        assertThat(updated.lastUpdatedAt).isEqualTo(result)
        assertThat(Instant.now().toEpochMilli() - updated.lastUpdatedAt.toEpochMilli()).isLessThan(100)
        assertThat(updated.version!! - item.version!!).isEqualTo(1)
    }

    @Test
    fun `update lastUpdatedAt on non existent item`() = runBlocking<Unit> {
        val itemId = ShortItemId(randomEthItemId())

        itemRepository.updateLastUpdatedAt(itemId)

        assertThat(itemRepository.get(itemId)).isNull()
    }
}
