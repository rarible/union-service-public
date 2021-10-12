package com.rarible.protocol.union.listener.job

import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.union.core.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomShortOwnership
import com.rarible.protocol.union.enrichment.util.bidCurrencyId
import com.rarible.protocol.union.enrichment.util.sellCurrencyId
import com.rarible.protocol.union.listener.service.EnrichmentItemEventService
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import com.rarible.protocol.union.listener.test.data.createCurrencyDto
import com.rarible.protocol.union.test.data.randomEthItemId
import com.rarible.protocol.union.test.data.randomEthLegacyOrderDto
import com.rarible.protocol.union.test.data.randomEthNftItemDto
import com.rarible.protocol.union.test.data.randomEthOwnershipDto
import com.rarible.protocol.union.test.data.randomEthOwnershipId
import com.rarible.protocol.union.test.data.randomUnionOrderDto
import io.mockk.clearMocks
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono
import java.math.BigDecimal
import java.time.Instant

@FlowPreview
@IntegrationTest
internal class PriceUpdateJobTest : AbstractIntegrationTest() {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var itemEventService: EnrichmentItemEventService

    @Autowired
    private lateinit var currencyControllerApi: CurrencyControllerApi

    @Autowired
    private lateinit var itemService: EnrichmentItemService

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var ownershipService: EnrichmentOwnershipService

    @Autowired
    private lateinit var ownershipRepository: OwnershipRepository

    @Autowired
    private lateinit var priceUpdateJob: BestOrderCheckJob

    @Autowired
    lateinit var ethOrderConverter: EthOrderConverter

    @BeforeEach
    fun beforeEach() = runBlocking<Unit> {
        itemRepository.createIndexes()
        ownershipRepository.createIndices()
        clearMocks(
            currencyControllerApi,
            testEthereumOrderApi,
            testEthereumOwnershipApi
        )
    }

    @Test
    fun `should update best order for multi orders items`() = runBlocking<Unit> {
        val itemId = randomEthItemId()

        val unionSellOrder1 = randomUnionOrderDto(randomEthItemId())
        val unionSellOrder2 = randomUnionOrderDto(randomEthItemId())
        val sellOrder1 = ShortOrderConverter.convert(unionSellOrder1).copy(makePrice = BigDecimal.valueOf(1))
        val sellOrder2 = ShortOrderConverter.convert(unionSellOrder2).copy(makePrice = BigDecimal.valueOf(2))

        val unionBidOrder1 = randomUnionOrderDto(randomEthItemId())
        val unionBidOrder2 = randomUnionOrderDto(randomEthItemId())
        val bidOrder1 = ShortOrderConverter.convert(unionBidOrder1).copy(takePrice = BigDecimal.valueOf(2))
        val bidOrder2 = ShortOrderConverter.convert(unionBidOrder2).copy(takePrice = BigDecimal.valueOf(1))

        val shortItem = randomShortItem(itemId).copy(
            bestSellOrder = sellOrder2,
            bestSellOrders = mapOf(
                unionSellOrder1.sellCurrencyId to sellOrder1,
                unionSellOrder2.sellCurrencyId to sellOrder2
            ),
            bestBidOrder = bidOrder2,
            bestBidOrders = mapOf(unionBidOrder1.bidCurrencyId to bidOrder1, unionBidOrder2.bidCurrencyId to bidOrder2),
            multiCurrency = true,
            lastUpdatedAt = Instant.EPOCH
        )

        coEvery { currencyControllerApi.getCurrencyRate(any(), any(), any()) } returns createCurrencyDto().copy(rate = BigDecimal.ONE).toMono()
        coEvery { testEthereumItemApi.getNftItemById(itemId.value) } returns randomEthNftItemDto().toMono()
        coEvery { testEthereumOrderApi.getOrderByHash(any()) } returns randomEthLegacyOrderDto().toMono()

        itemRepository.save(shortItem)
        priceUpdateJob.updateBestOrderPrice()

        val updatedItem = itemService.get(shortItem.id)
        assertThat(updatedItem).isNotNull

        assertThat(updatedItem?.bestSellOrder?.id).isEqualTo(sellOrder1.id)
        assertThat(updatedItem?.bestBidOrder?.id).isEqualTo(bidOrder1.id)
    }

    @Test
    fun `should update best order for multi orders ownership`() = runBlocking<Unit> {
        val ownershipId = randomEthOwnershipId()

        val unionSellOrder1 = randomUnionOrderDto(randomEthItemId())
        val unionSellOrder2 = randomUnionOrderDto(randomEthItemId())
        val sellOrder1 = ShortOrderConverter.convert(unionSellOrder1).copy(makePrice = BigDecimal.valueOf(1))
        val sellOrder2 = ShortOrderConverter.convert(unionSellOrder2).copy(makePrice = BigDecimal.valueOf(2))

        val shortOwnership = randomShortOwnership(ownershipId).copy(
            bestSellOrder = sellOrder2,
            bestSellOrders = mapOf(
                unionSellOrder1.sellCurrencyId to sellOrder1,
                unionSellOrder2.sellCurrencyId to sellOrder2
            ),
            multiCurrency = true,
            lastUpdatedAt = Instant.EPOCH
        )

        coEvery { currencyControllerApi.getCurrencyRate(any(), any(), any()) } returns createCurrencyDto().copy(rate = BigDecimal.ONE).toMono()
        coEvery { testEthereumOwnershipApi.getNftOwnershipById(ownershipId.value) } returns randomEthOwnershipDto().toMono()
        coEvery { testEthereumOrderApi.getOrderByHash(any()) } returns randomEthLegacyOrderDto().toMono()

        ownershipRepository.save(shortOwnership)
        priceUpdateJob.updateBestOrderPrice()

        val updatedItem = ownershipService.get(shortOwnership.id)
        assertThat(updatedItem).isNotNull

        assertThat(updatedItem?.bestSellOrder?.id).isEqualTo(sellOrder1.id)
    }
}
