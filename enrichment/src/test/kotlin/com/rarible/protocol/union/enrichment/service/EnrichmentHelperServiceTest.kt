package com.rarible.protocol.union.enrichment.service

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.enrichment.configuration.EnrichmentCurrenciesProperties
import com.rarible.protocol.union.enrichment.test.data.randomEnrichmentCollection
import com.rarible.protocol.union.enrichment.test.data.randomShortBidOrder
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomShortSellOrder
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import randomContractAddress

@ExtendWith(MockKExtension::class)
internal class EnrichmentHelperServiceTest {

    @InjectMockKs
    private lateinit var enrichmentHelperService: EnrichmentHelperService

    private val whitelistedCurrency = ContractAddress(BlockchainDto.ETHEREUM, randomAddress().toString())

    @SpyK
    private var featureFlagsProperties = FeatureFlagsProperties(enableItemBestBidsByCurrency = true)

    @SpyK
    private var enrichmentCurrencyProperties = EnrichmentCurrenciesProperties(
        bestBidByCurrencyWhitelist = listOf(whitelistedCurrency.fullId())
    )

    @Test
    fun `getExistingOrders - ok, no orders`() {
        val collection = randomEnrichmentCollection()
        assertThat(enrichmentHelperService.getExistingOrders(collection)).isEmpty()
        assertThat(enrichmentHelperService.getExistingOrders(null)).isEmpty()
    }

    @Test
    fun `getExistingOrders - ok, without whitelisted currencies`() = runBlocking<Unit> {
        val bestBidOrder = randomShortBidOrder()
        val bestSellOrder = randomShortSellOrder()
        val item = randomShortItem().copy(
            bestBidOrder = bestBidOrder,
            bestSellOrder = bestSellOrder,
            bestBidOrders = mapOf(randomContractAddress().value to bestBidOrder)
        )
        assertThat(enrichmentHelperService.getExistingOrders(item)).containsExactlyInAnyOrder(
            bestSellOrder,
            bestBidOrder
        )
    }

    @Test
    fun `getExistingOrders - ok, with whitelisted currencies`() = runBlocking<Unit> {
        val bestBidOrder = randomShortBidOrder()
        val bestCurrencyBidOrder = randomShortBidOrder()
        val bestCurrencyBidOrderWl = randomShortBidOrder()
        val bestSellOrder = randomShortSellOrder()
        val item = randomShortItem().copy(
            bestBidOrder = bestBidOrder,
            bestSellOrder = bestSellOrder,
            bestBidOrders = mapOf(
                randomContractAddress().value to bestBidOrder, // Ok, the best one
                randomContractAddress().value to bestCurrencyBidOrder, // Skipped, currency
                whitelistedCurrency.value to bestCurrencyBidOrderWl // Ok, whitelisted
            )
        )
        assertThat(enrichmentHelperService.getExistingOrders(item)).containsExactlyInAnyOrder(
            bestSellOrder,
            bestBidOrder,
            bestCurrencyBidOrderWl
        )
    }

    @Test
    fun `getExistingOrders - batch`() = runBlocking<Unit> {
        val bestBidOrder1 = randomShortBidOrder()
        val bestCurrencyBidOrder1 = randomShortBidOrder()
        val bestCurrencyBidOrderWl1 = randomShortBidOrder()
        val bestSellOrder1 = randomShortSellOrder()
        val item1 = randomShortItem().copy(
            bestBidOrder = bestBidOrder1,
            bestSellOrder = bestSellOrder1,
            bestBidOrders = mapOf(
                randomContractAddress().value to bestBidOrder1, // Ok, the best one
                randomContractAddress().value to bestCurrencyBidOrder1, // Skipped, currency
                whitelistedCurrency.value to bestCurrencyBidOrderWl1 // Ok, whitelisted
            )
        )

        val bestBidOrder2 = randomShortBidOrder()
        val bestSellOrder2 = randomShortSellOrder()
        val item2 = randomShortItem().copy(
            bestBidOrder = bestBidOrder2,
            bestSellOrder = bestSellOrder2,
            bestBidOrders = mapOf(randomContractAddress().value to bestBidOrder2)
        )

        assertThat(enrichmentHelperService.getExistingOrders(listOf(item1, item2))).containsExactlyInAnyOrder(
            bestSellOrder2,
            bestBidOrder2,
            bestSellOrder1,
            bestBidOrder1,
            bestCurrencyBidOrderWl1
        )
    }
}
