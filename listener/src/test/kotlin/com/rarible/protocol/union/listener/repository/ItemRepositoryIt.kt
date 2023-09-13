package com.rarible.protocol.union.listener.repository

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.ShortOrder
import com.rarible.protocol.union.enrichment.model.ShortPoolOrder
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrder
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
import scalether.domain.Address
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
        val itemId1 = ItemIdDto(BlockchainDto.ETHEREUM, "${Address.ONE().hex()}:0")
        val itemId3 = ItemIdDto(BlockchainDto.ETHEREUM, "${Address.ONE().hex()}:3")
        val itemId5 = ItemIdDto(BlockchainDto.ETHEREUM, "${Address.ONE().hex()}:5")
        val itemId6 = ItemIdDto(BlockchainDto.ETHEREUM, "${Address.ONE().hex()}:6")

        val now = nowMillis()

        val item1 = itemRepository.save(
            randomShortItem(itemId1).copy(bestSellOrder = randomSellOrder(PlatformDto.OPEN_SEA), lastUpdatedAt = now)
        )
        val item2 = itemRepository.save(
            randomShortItem().copy(bestSellOrder = randomSellOrder(PlatformDto.RARIBLE), lastUpdatedAt = now)
        )
        val item3 = itemRepository.save(
            randomShortItem(itemId3).copy(bestSellOrder = randomSellOrder(PlatformDto.OPEN_SEA), lastUpdatedAt = now)
        )
        val item4 = itemRepository.save(
            randomShortItem().copy(bestSellOrder = randomSellOrder(PlatformDto.RARIBLE), lastUpdatedAt = now)
        )
        val item5 = itemRepository.save(
            randomShortItem(itemId5).copy(bestSellOrder = randomSellOrder(PlatformDto.OPEN_SEA), lastUpdatedAt = now)
        )
        val item6 = itemRepository.save(
            randomShortItem(itemId6).copy(bestSellOrder = randomSellOrder(PlatformDto.OPEN_SEA), lastUpdatedAt = now)
        )
        val item7 = itemRepository.save(randomShortItem().copy(bestSellOrder = null, lastUpdatedAt = now))

        val openSeaItems = itemRepository.findByPlatformWithSell(PlatformDto.OPEN_SEA, null, null).toList()
        assertThat(openSeaItems.map { it.id }).containsExactly(item1.id, item3.id, item5.id, item6.id)

        val fromOpenSeaItems = itemRepository.findByPlatformWithSell(PlatformDto.OPEN_SEA, openSeaItems[1].id, null)
            .toList()
        assertThat(fromOpenSeaItems.size).isEqualTo(2)
        assertThat(fromOpenSeaItems.map { it.id }).contains(openSeaItems[2].id, openSeaItems[3].id)
    }

    @Test
    fun `find all by blockchain`() = runBlocking<Unit> {
        val item1 = itemRepository.save(randomShortItem().copy(blockchain = BlockchainDto.TEZOS, itemId = "aaa:1"))
        val item2 = itemRepository.save(randomShortItem().copy(blockchain = BlockchainDto.TEZOS, itemId = "bbb:2"))
        val item3 = itemRepository.save(randomShortItem().copy(blockchain = BlockchainDto.TEZOS, itemId = "ccc:3"))
        itemRepository.save(randomShortItem().copy(blockchain = BlockchainDto.FLOW))

        val firstPage = itemRepository.findByBlockchain(null, BlockchainDto.TEZOS, 2).toList()
        assertThat(firstPage).isEqualTo(listOf(item1, item2))

        val withOffset = itemRepository.findByBlockchain(item1.id, BlockchainDto.TEZOS, 3).toList()
        assertThat(withOffset).isEqualTo(listOf(item2, item3))
    }

    private fun randomSellOrder(platform: PlatformDto): ShortOrder {
        val randomSellOrder = randomUnionSellOrder(randomEthItemId())
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
    fun findIdsByLastUpdatedAt() = runBlocking<Unit> {
        val item1 = itemRepository.save(
            randomShortItem().copy(
                lastUpdatedAt = Instant.ofEpochMilli(1000),
                blockchain = BlockchainDto.ETHEREUM,
                itemId = "1"
            )
        )
        val item2 = itemRepository.save(
            randomShortItem().copy(
                lastUpdatedAt = Instant.ofEpochMilli(2000),
                blockchain = BlockchainDto.ETHEREUM,
                itemId = "2"
            )
        )
        val item3 = itemRepository.save(
            randomShortItem().copy(
                lastUpdatedAt = Instant.ofEpochMilli(3000),
                blockchain = BlockchainDto.ETHEREUM,
                itemId = "3"
            )
        )
        val item4 = itemRepository.save(
            randomShortItem().copy(
                lastUpdatedAt = Instant.ofEpochMilli(4000),
                blockchain = BlockchainDto.ETHEREUM,
                itemId = "4"
            )
        )
        val item5 = itemRepository.save(
            randomShortItem().copy(
                lastUpdatedAt = Instant.ofEpochMilli(4000),
                blockchain = BlockchainDto.ETHEREUM,
                itemId = "5"
            )
        )
        val item6 = itemRepository.save(
            randomShortItem().copy(
                lastUpdatedAt = Instant.ofEpochMilli(6000),
                blockchain = BlockchainDto.ETHEREUM,
                itemId = "6"
            )
        )
        itemRepository.save(
            randomShortItem().copy(
                lastUpdatedAt = Instant.ofEpochMilli(8000),
                blockchain = BlockchainDto.ETHEREUM,
                itemId = "7"
            )
        )

        var result = itemRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = Instant.ofEpochMilli(500),
            lastUpdatedTo = Instant.ofEpochMilli(7000),
            fromId = null,
            size = 2
        )
        assertThat(result.map { it.id }).containsExactlyInAnyOrder(item1.id, item2.id)

        result = itemRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = result.last().lastUpdatedAt,
            lastUpdatedTo = Instant.ofEpochMilli(7000),
            fromId = result.last().id,
            size = 2
        )
        assertThat(result.map { it.id }).containsExactlyInAnyOrder(item3.id, item4.id)

        result = itemRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = result.last().lastUpdatedAt,
            lastUpdatedTo = Instant.ofEpochMilli(7000),
            fromId = result.last().id,
            size = 10
        )
        assertThat(result.map { it.id }).containsExactlyInAnyOrder(item5.id, item6.id)
    }

    @Test
    fun findAllFromIdExcluded() = runBlocking<Unit> {
        val item1 = itemRepository.save(
            randomShortItem(
                id = ItemIdDto(
                    blockchain = BlockchainDto.ETHEREUM,
                    value = "${Address.ONE()}:1"
                )
            )
        )
        val item2 = itemRepository.save(
            randomShortItem(
                id = ItemIdDto(
                    blockchain = BlockchainDto.ETHEREUM,
                    value = "${Address.ONE()}:2"
                )
            )
        )
        val item3 = itemRepository.save(
            randomShortItem(
                id = ItemIdDto(
                    blockchain = BlockchainDto.ETHEREUM,
                    value = "${Address.ONE()}:3"
                )
            )
        )
        val item4 = itemRepository.save(
            randomShortItem(
                id = ItemIdDto(
                    blockchain = BlockchainDto.ETHEREUM,
                    value = "${Address.ONE()}:4"
                )
            )
        )
        val item5 = itemRepository.save(
            randomShortItem(
                id = ItemIdDto(
                    blockchain = BlockchainDto.ETHEREUM,
                    value = "${Address.ONE()}:5"
                )
            )
        )

        assertThat(itemRepository.findAll().toList()).containsExactly(item1, item2, item3, item4, item5)

        assertThat(itemRepository.findAll(fromIdExcluded = item2.id).toList()).containsExactly(item3, item4, item5)
    }
}
