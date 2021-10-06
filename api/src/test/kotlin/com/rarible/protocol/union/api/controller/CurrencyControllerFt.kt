package com.rarible.protocol.union.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.protocol.currency.dto.BlockchainDto
import com.rarible.protocol.currency.dto.CurrencyRateDto
import com.rarible.protocol.union.api.client.CurrencyControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.test.data.randomAddressString
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@FlowPreview
@IntegrationTest
class CurrencyControllerFt : AbstractIntegrationTest() {

    @Autowired
    lateinit var currencyControllerApi: CurrencyControllerApi

    @Test
    fun `get currency usd rate`() = runBlocking<Unit> {
        val address = randomAddressString()
        val now = nowMillis()

        val currencyDto = CurrencyRateDto(address, "usd", randomBigDecimal(), now)

        coEvery {
            testCurrencyApi.getCurrencyRate(BlockchainDto.ETHEREUM, address, now.toEpochMilli())
        } returns currencyDto.toMono()

        val result = currencyControllerApi.getCurrencyUsdRate(
            com.rarible.protocol.union.dto.BlockchainDto.ETHEREUM,
            address,
            now
        ).awaitFirst()

        assertThat(result.currencyId).isEqualTo(address)
        assertThat(result.date).isEqualTo(now)
        assertThat(result.rate).isEqualTo(currencyDto.rate)
    }

    @Test
    fun `get currency usd rate - currency not found`() = runBlocking<Unit> {
        val address = randomAddressString()
        val now = nowMillis()

        coEvery {
            testCurrencyApi.getCurrencyRate(BlockchainDto.ETHEREUM, address, now.toEpochMilli())
        } returns Mono.empty()

        val ex = assertThrows<CurrencyControllerApi.ErrorGetCurrencyUsdRate> {
            runBlocking {
                currencyControllerApi.getCurrencyUsdRate(
                    com.rarible.protocol.union.dto.BlockchainDto.ETHEREUM,
                    address,
                    now
                ).awaitFirst()
            }
        }

        assertThat(ex.on400).isNotNull
    }

}