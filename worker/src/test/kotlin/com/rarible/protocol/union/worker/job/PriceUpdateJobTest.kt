package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomShortOwnership
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrder

import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthBidOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import com.rarible.protocol.union.integration.ethereum.data.randomEthSellOrderDto
import com.rarible.protocol.union.worker.AbstractIntegrationTest
import com.rarible.protocol.union.worker.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.Instant

@IntegrationTest
internal class PriceUpdateJobTest: AbstractIntegrationTest() {
    @Autowired
    private lateinit var ethereumOrderConverter: EthOrderConverter

    @Autowired
    private lateinit var itemService: EnrichmentItemService

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var ownershipService: EnrichmentOwnershipService

    @Autowired
    private lateinit var ownershipRepository: OwnershipRepository

    @Autowired
    private lateinit var priceUpdateJob: BestOrderCheckJobHandler

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        itemRepository.createIndices()
        ownershipRepository.createIndices()
    }

    @Test
    fun `should update best order for multi orders items`() = runBlocking<Unit> {
        val itemId = randomEthItemId()

        val ethSellOrder1 = randomEthSellOrderDto(randomEthItemId())
        val ethSellOrder2 = randomEthSellOrderDto(randomEthItemId())
        val unionSellOrder1 = ethereumOrderConverter.convert(ethSellOrder1, BlockchainDto.ETHEREUM)
        val unionSellOrder2 = ethereumOrderConverter.convert(ethSellOrder2, BlockchainDto.ETHEREUM)
        val sellOrder1 = ShortOrderConverter.convert(unionSellOrder1).copy(makePrice = BigDecimal.valueOf(1))
        val sellOrder2 = ShortOrderConverter.convert(unionSellOrder2).copy(makePrice = BigDecimal.valueOf(2))

        val ethBidOrder1 = randomEthBidOrderDto(randomEthItemId())
        val ethBidOrder2 = randomEthBidOrderDto(randomEthItemId())
        val unionBidOrder1 = ethereumOrderConverter.convert(ethBidOrder1, BlockchainDto.ETHEREUM)
        val unionBidOrder2 = ethereumOrderConverter.convert(ethBidOrder2, BlockchainDto.ETHEREUM)
        val bidOrder1 = ShortOrderConverter.convert(unionBidOrder1).copy(takePrice = BigDecimal.valueOf(2))
        val bidOrder2 = ShortOrderConverter.convert(unionBidOrder2).copy(takePrice = BigDecimal.valueOf(1))

        val shortItem = randomShortItem(itemId).copy(
            bestSellOrder = sellOrder2,
            bestSellOrders = mapOf(
                unionSellOrder1.sellCurrencyId() to sellOrder1,
                unionSellOrder2.sellCurrencyId() to sellOrder2
            ),
            bestBidOrder = bidOrder2,
            bestBidOrders = mapOf(
                unionBidOrder1.bidCurrencyId() to bidOrder1,
                unionBidOrder2.bidCurrencyId() to bidOrder2
            ),
            multiCurrency = true,
            lastUpdatedAt = Instant.EPOCH
        )

        val ethItem = randomEthNftItemDto()
        ethereumItemControllerApiMock.mockGetNftItemById(itemId, ethItem)
        ethereumItemControllerApiMock.mockGetNftItemMetaById(itemId, randomEthItemMeta())
        ethereumOrderControllerApiMock.mockGetByIds(ethSellOrder1, ethBidOrder1)

        itemRepository.save(shortItem)
        priceUpdateJob.handle()

        val updatedItem = itemService.get(shortItem.id)
        assertThat(updatedItem).isNotNull

        assertThat(updatedItem?.bestSellOrder?.id).isEqualTo(sellOrder1.id)
        assertThat(updatedItem?.bestBidOrder?.id).isEqualTo(bidOrder1.id)
    }

    @Test
    fun `should update best order for multi orders ownership`() = runBlocking<Unit> {
        val ownershipId = randomEthOwnershipId()

        val unionSellOrder1 = randomUnionSellOrder(randomEthItemId())
        val unionSellOrder2 = randomUnionSellOrder(randomEthItemId())
        val sellOrder1 = ShortOrderConverter.convert(unionSellOrder1).copy(makePrice = BigDecimal.valueOf(1))
        val sellOrder2 = ShortOrderConverter.convert(unionSellOrder2).copy(makePrice = BigDecimal.valueOf(2))

        val shortOwnership = randomShortOwnership(ownershipId).copy(
            bestSellOrder = sellOrder2,
            bestSellOrders = mapOf(
                unionSellOrder1.sellCurrencyId() to sellOrder1,
                unionSellOrder2.sellCurrencyId() to sellOrder2
            ),
            multiCurrency = true,
            lastUpdatedAt = Instant.EPOCH
        )

        ownershipRepository.save(shortOwnership)
        priceUpdateJob.handle()

        val updatedItem = ownershipService.get(shortOwnership.id)
        assertThat(updatedItem).isNotNull

        assertThat(updatedItem?.bestSellOrder?.id).isEqualTo(sellOrder1.id)
    }
}
