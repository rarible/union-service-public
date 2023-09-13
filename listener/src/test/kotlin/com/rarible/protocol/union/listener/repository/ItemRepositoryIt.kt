package com.rarible.protocol.union.listener.repository

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.ShortItem
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
    fun `find by platform with sell order - ok`() = runBlocking<Unit> {
        val itemId3 = ItemIdDto(BlockchainDto.ETHEREUM, "${Address.ONE().hex()}:3")
        val itemId6 = ItemIdDto(BlockchainDto.ETHEREUM, "${Address.ONE().hex()}:6")

        val now = nowMillis().minusSeconds(1)

        val item1 = randomShortItem(randomEthItemId(), now.minusSeconds(1), PlatformDto.OPEN_SEA)
        val item2 = randomShortItem(randomEthItemId(), now, PlatformDto.RARIBLE)
        val item3 = randomShortItem(itemId3, now, PlatformDto.OPEN_SEA)
        val item4 = randomShortItem(randomEthItemId(), now, PlatformDto.RARIBLE)
        val item5 = randomShortItem(randomEthItemId(), now.minusSeconds(3), PlatformDto.OPEN_SEA)
        val item6 = randomShortItem(itemId6, now, PlatformDto.OPEN_SEA)
        val item7 = randomShortItem(randomEthItemId(), now, null)

        val openSeaItems = itemRepository.findByPlatformWithSell(
            platform = PlatformDto.OPEN_SEA,
            fromItemId = null,
            fromLastUpdatedAt = Instant.now(),
            limit = null
        ).toList()

        assertThat(openSeaItems.map { it.id }).isEqualTo(listOf(item3.id, item6.id, item1.id, item5.id))

        val fromOpenSeaItems = itemRepository.findByPlatformWithSell(
            PlatformDto.OPEN_SEA,
            fromItemId = item3.id,
            fromLastUpdatedAt = item3.lastUpdatedAt
        ).toList()

        assertThat(fromOpenSeaItems.map { it.id }).isEqualTo(listOf(item6.id, item1.id, item5.id))
    }

    private suspend fun randomShortItem(
        itemId: ItemIdDto,
        lastUpdatedAt: Instant,
        platform: PlatformDto? = null,
    ): ShortItem {
        return itemRepository.save(
            randomShortItem(itemId).copy(
                bestSellOrder = platform?.let { randomSellOrder(platform) },
                lastUpdatedAt = lastUpdatedAt
            )
        )
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
