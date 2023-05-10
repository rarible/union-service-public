package com.rarible.protocol.union.enrichment.service

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.protocol.dto.AssetDto
import com.rarible.protocol.dto.Erc20AssetTypeDto
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.UnionAssetType
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CurrencyIdDto
import com.rarible.protocol.union.enrichment.configuration.EnrichmentCurrenciesProperties
import com.rarible.protocol.union.enrichment.configuration.EnrichmentProperties
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.test.data.randomEnrichmentCollection
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthBidOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthSellOrderDto
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import scalether.domain.Address
import java.math.BigDecimal

@ExtendWith(MockKExtension::class)
internal class EnrichmentHelperServiceTest {
    @InjectMockKs
    private lateinit var enrichmentHelperService: EnrichmentHelperService

    private val whitelistedCurrency =
        CurrencyIdDto(blockchain = BlockchainDto.ETHEREUM, contract = randomAddress().toString(), tokenId = null)

    @SpyK
    private var featureFlagsProperties = FeatureFlagsProperties(enableItemBestBidsByCurrency = true)

    @SpyK
    private var enrichmentProperties =
        EnrichmentProperties(
            currencies = EnrichmentCurrenciesProperties(
                bestBidByCurrencyWhitelist = listOf(
                    whitelistedCurrency.fullId()
                )
            )
        )

    private val currencyService = mockk<CurrencyService>()
    private val ethOrderConverter = EthOrderConverter(
        currencyService = currencyService,
    )

    @Test
    fun `getExistingOrders no orders`() {
        val collection = randomEnrichmentCollection()
        assertThat(enrichmentHelperService.getExistingOrders(collection)).isEmpty()
        assertThat(enrichmentHelperService.getExistingOrders(null)).isEmpty()
    }

    @Test
    fun `getExistingOrders no bids by currency`() = runBlocking<Unit> {
        coEvery {
            currencyService.toUsd(
                any(),
                any<UnionAssetType>(),
                any(),
                any()
            )
        } returns BigDecimal.ONE
        val itemId = randomEthItemId()
        val bestSellOrder =
            ShortOrderConverter.convert(ethOrderConverter.convert(randomEthSellOrderDto(itemId), itemId.blockchain))
        val unionBestBidOrder = ethOrderConverter.convert(randomEthBidOrderDto(itemId), itemId.blockchain)
        val bestBidOrder = ShortOrderConverter.convert(unionBestBidOrder)
        val item = randomShortItem(itemId).copy(
            bestBidOrder = bestBidOrder,
            bestSellOrder = bestSellOrder,
            bestBidOrders = mapOf(unionBestBidOrder.bidCurrencyId() to bestBidOrder)
        )
        assertThat(enrichmentHelperService.getExistingOrders(item)).containsExactlyInAnyOrder(
            bestSellOrder,
            bestBidOrder
        )
    }

    @Test
    fun `getExistingOrders with bids by currency`() = runBlocking<Unit> {
        coEvery {
            currencyService.toUsd(
                any(),
                any<UnionAssetType>(),
                any(),
                any()
            )
        } returns BigDecimal.ONE
        val itemId = randomEthItemId()
        val bestSellOrder =
            ShortOrderConverter.convert(ethOrderConverter.convert(randomEthSellOrderDto(itemId), itemId.blockchain))
        val unionBestBidOrder = ethOrderConverter.convert(randomEthBidOrderDto(itemId), itemId.blockchain)
        val bestBidOrder = ShortOrderConverter.convert(unionBestBidOrder)
        val currencyUnionBidOrder1 = ethOrderConverter.convert(randomEthBidOrderDto(itemId), itemId.blockchain)
        val currencyBidOrder1 = ShortOrderConverter.convert(currencyUnionBidOrder1)
        val currencyUnionBidOrder2 = ethOrderConverter.convert(
            randomEthBidOrderDto(itemId).copy(
                make = AssetDto(
                    assetType = Erc20AssetTypeDto(Address.apply(whitelistedCurrency.contract)),
                    value = randomBigInt(),
                    valueDecimal = randomBigDecimal()
                )
            ), itemId.blockchain
        )
        val currencyBidOrder2 = ShortOrderConverter.convert(currencyUnionBidOrder2)
        val item = randomShortItem(itemId).copy(
            bestBidOrder = bestBidOrder,
            bestSellOrder = bestSellOrder,
            bestBidOrders = mapOf(
                unionBestBidOrder.bidCurrencyId() to bestBidOrder,
                currencyUnionBidOrder1.bidCurrencyId() to currencyBidOrder1,
                currencyUnionBidOrder2.bidCurrencyId() to currencyBidOrder2
            )
        )
        assertThat(enrichmentHelperService.getExistingOrders(item)).containsExactlyInAnyOrder(
            bestSellOrder,
            bestBidOrder,
            currencyBidOrder2
        )
    }
}