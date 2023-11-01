package com.rarible.protocol.union.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.protocol.currency.dto.CurrenciesDto
import com.rarible.protocol.currency.dto.CurrencyRateDto
import com.rarible.protocol.union.api.client.CurrencyControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.test.nativeTestCurrencies
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CurrencyIdDto
import com.rarible.protocol.union.integration.ethereum.data.randomAddressString
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.stream.Stream

@FlowPreview
@IntegrationTest
class CurrencyControllerFt : AbstractIntegrationTest() {

    @Autowired
    lateinit var currencyControllerApi: CurrencyControllerApi

    @ParameterizedTest
    @MethodSource("currencyId")
    fun `get currency usd rate`(currencyId: CurrencyIdDto) = runBlocking<Unit> {
        val now = nowMillis()
        val currencyDto = CurrencyRateDto(currencyId.contract, "usd", randomBigDecimal(), now)

        coEvery {
            testCurrencyApi.getCurrencyRate(BlockchainDto.ETHEREUM.name, currencyId.value, now.toEpochMilli())
        } returns currencyDto.toMono()

        val result = currencyControllerApi.getUsdRate(
            currencyId.fullId(),
            now
        ).awaitFirst()

        assertThat(result.currencyId).isEqualTo(currencyId.contract)
        assertThat(result.date).isEqualTo(now)
        assertThat(result.rate).isEqualTo(currencyDto.rate.stripTrailingZeros())
    }

    @ParameterizedTest
    @MethodSource("currencyId")
    fun `get currency usd rate - currency not found`(currencyId: CurrencyIdDto) = runBlocking<Unit> {
        val now = nowMillis()

        coEvery {
            testCurrencyApi.getCurrencyRate(BlockchainDto.ETHEREUM.name, currencyId.value, now.toEpochMilli())
        } returns Mono.empty()

        val ex = assertThrows<CurrencyControllerApi.ErrorGetUsdRate> {
            runBlocking {
                currencyControllerApi.getUsdRate(currencyId.fullId(), now).awaitFirst()
            }
        }
        assertThat(ex.on400).isNotNull
    }

    @Test
    fun `get all currencies`() = runBlocking<Unit> {
        every { testCurrencyApi.allCurrencies } returns Mono.just(CurrenciesDto(nativeTestCurrencies()))

        val currencies = currencyControllerApi.allCurrencies.awaitFirst().currencies

        assertThat(currencies).hasSize(14)
        assertThat(currencies).doesNotHaveDuplicates()
    }

    companion object {

        @JvmStatic
        fun currencyId() = Stream.of(
            CurrencyIdDto(
                BlockchainDto.ETHEREUM,
                randomAddressString(),
                randomBigInt()
            ),
            CurrencyIdDto(
                BlockchainDto.ETHEREUM,
                randomAddressString(),
                null
            )
        )
    }
}
