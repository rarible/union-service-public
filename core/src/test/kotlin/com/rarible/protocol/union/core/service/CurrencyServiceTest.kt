package com.rarible.protocol.union.core.service

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomString
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.CurrencyRateDto
import com.rarible.protocol.union.core.client.CurrencyClient
import com.rarible.protocol.union.core.converter.CurrencyConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EthErc20AssetTypeDto
import com.rarible.protocol.union.dto.UnionAddress
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.math.BigDecimal

class CurrencyServiceTest {

    private val currencyControllerApi: CurrencyControllerApi = mockk()
    private val currencyService = CurrencyService(CurrencyClient(currencyControllerApi))

    @BeforeEach
    fun beforeEach() {
        clearMocks(currencyControllerApi)
        currencyService.invalidateCache()
    }

    @Test
    fun `get rate`() = runBlocking<Unit> {
        val rate = randomBigDecimal()
        val blockchain = BlockchainDto.ETHEREUM
        val address = randomString()
        mockCurrency(blockchain, address, rate)

        val usdRate1 = currencyService.getRate(blockchain, address, nowMillis())
        val usdRate2 = currencyService.getRate(blockchain, address, nowMillis())

        assertThat(usdRate1.rate).isEqualTo(rate)
        assertThat(usdRate2.rate).isEqualTo(rate)

        // This request is not cached, so should be executed twice
        verifyCurrency(blockchain, address, 2)
    }

    @Test
    fun `to usd - actual`() = runBlocking<Unit> {
        val rate = randomBigDecimal()
        val blockchain = BlockchainDto.ETHEREUM
        val address = randomString()
        mockCurrency(blockchain, address, rate, BigDecimal.ONE)

        val at = nowMillis().minusSeconds(60 * 29)

        val assetType = EthErc20AssetTypeDto(UnionAddress(blockchain, address))
        val usdRate1 = currencyService.toUsd(blockchain, assetType, BigDecimal.ONE, at)
        val usdRate2 = currencyService.toUsd(blockchain, assetType, BigDecimal.ONE, at)

        // Both times should be rate * BigDecimal.ONE
        assertThat(usdRate1).isEqualTo(rate)
        assertThat(usdRate2).isEqualTo(rate)

        // Both times we requested rate in "actual" time rage - so only 1 call should be here
        verifyCurrency(blockchain, address, 1)
    }

    @Test
    fun `to usd - historical`() = runBlocking<Unit> {
        val rate = randomBigDecimal()
        val blockchain = BlockchainDto.ETHEREUM
        val address = randomString()
        mockCurrency(blockchain, address, rate, BigDecimal.ONE)

        val at = nowMillis().minusSeconds(60 * 29)

        val assetType = EthErc20AssetTypeDto(UnionAddress(blockchain, address))
        val usdRate1 = currencyService.toUsd(blockchain, assetType, BigDecimal.ONE, at)
        val usdRate2 = currencyService.toUsd(blockchain, assetType, BigDecimal.ONE, at.minusSeconds(2 * 60))

        // Second request should return rate == 1
        assertThat(usdRate1).isEqualTo(rate)
        assertThat(usdRate2).isEqualTo(BigDecimal.ONE)

        // Both times we requested rate in "actual" time rage - so only 1 call should be here
        verifyCurrency(blockchain, address, 2)
    }

    @Test
    fun `get current rate`() = runBlocking<Unit> {
        val rate = randomBigDecimal()
        val blockchain = BlockchainDto.FLOW
        val address = randomString()
        mockCurrency(blockchain, address, rate)

        val usdRate1 = currencyService.getCurrentRate(blockchain, address)!!
        val usdRate2 = currencyService.getCurrentRate(blockchain, address)!!

        assertThat(usdRate1.rate).isEqualTo(rate)
        assertThat(usdRate2.rate).isEqualTo(rate)

        // This request is cached, so should be executed only once
        verifyCurrency(blockchain, address, 1)
    }

    @Test
    fun `get current rate - failed`() = runBlocking<Unit> {
        coEvery {
            currencyControllerApi.getCurrencyRate(any(), any(), any())
        } throws RuntimeException()

        val currency = currencyService.getCurrentRate(BlockchainDto.POLYGON, randomString())
        assertThat(currency).isNull()
    }

    @Test
    fun `refresh cache`() = runBlocking<Unit> {
        val ethRate1 = randomBigDecimal()
        val ethRate2 = randomBigDecimal()
        val ethBlockchain = BlockchainDto.ETHEREUM
        val ethAddress = randomString()

        val flowRate1 = randomBigDecimal()
        val flowBlockchain = BlockchainDto.FLOW
        val flowAddress = randomString()

        mockCurrency(ethBlockchain, ethAddress, ethRate1, ethRate2)
        mockCurrency(flowBlockchain, flowAddress, flowRate1, null)

        // Filling cache with initial values
        val currentEthRate = currencyService.getCurrentRate(ethBlockchain, ethAddress)!!
        val currentFlowRate = currencyService.getCurrentRate(flowBlockchain, flowAddress)!!

        assertThat(currentEthRate.rate).isEqualTo(ethRate1)
        assertThat(currentFlowRate.rate).isEqualTo(flowRate1)

        // Refreshing cache - there should be new values
        currencyService.refreshCache()

        val refreshedEthRate = currencyService.getCurrentRate(ethBlockchain, ethAddress)!!
        val refreshedFlowRate = currencyService.getCurrentRate(flowBlockchain, flowAddress)!!

        // Eth rate should be updated, flow rate should stay the same since refresh failed for it
        assertThat(refreshedEthRate.rate).isEqualTo(ethRate2)
        assertThat(refreshedFlowRate.rate).isEqualTo(flowRate1)

        verifyCurrency(ethBlockchain, ethAddress, 2)
        verifyCurrency(flowBlockchain, flowAddress, 2)
    }

    private fun mockCurrency(blockchain: BlockchainDto, address: String, vararg rates: BigDecimal?) {
        val mock = coEvery {
            currencyControllerApi.getCurrencyRate(
                eq(CurrencyConverter.convert(blockchain)),
                eq(address),
                any()
            )
        }
        rates.forEach {
            if (it != null) {
                mock.returns(
                    CurrencyRateDto(
                        fromCurrencyId = address,
                        toCurrencyId = "usd",
                        rate = it,
                        date = nowMillis()
                    ).toMono()
                )
            } else {
                mock.returns(Mono.empty())
            }
        }
    }

    private fun verifyCurrency(blockchain: BlockchainDto, address: String, count: Int) {
        coVerify(exactly = count) {
            currencyControllerApi.getCurrencyRate(
                eq(CurrencyConverter.convert(blockchain)),
                eq(address),
                any()
            )
        }
    }

}