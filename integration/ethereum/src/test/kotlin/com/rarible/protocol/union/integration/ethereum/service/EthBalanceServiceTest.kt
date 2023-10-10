package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.protocol.dto.Erc20DecimalBalanceDto
import com.rarible.protocol.dto.EthBalanceDto
import com.rarible.protocol.erc20.api.client.BalanceControllerApi
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthBalanceConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import reactor.kotlin.core.publisher.toMono

@ExtendWith(MockKExtension::class)
class EthBalanceServiceTest {

    @MockK
    private lateinit var balanceControllerApi: BalanceControllerApi

    private val blockchain = BlockchainDto.ETHEREUM

    @InjectMockKs
    private lateinit var balanceService: EthBalanceService

    @Test
    fun `get balance - ok, native currency`() = runBlocking<Unit> {
        val ethBalance = EthBalanceDto(
            owner = randomAddress(),
            balance = randomBigInt(),
            decimalBalance = randomBigDecimal()
        )
        val owner = EthConverter.convert(ethBalance.owner)

        every { balanceControllerApi.getEthBalance(owner) } returns ethBalance.toMono()

        val result = balanceService.getBalance(
            currencyId = EthBalanceConverter.NATIVE_CURRENCY_CONTRACT,
            owner = ethBalance.owner.prefixed()
        )

        assertThat(result.balance).isEqualTo(ethBalance.balance)
    }

    @Test
    fun `get balance - ok, erc20 currency`() = runBlocking<Unit> {
        val erc20Balance = Erc20DecimalBalanceDto(
            owner = randomAddress(),
            balance = randomBigInt(),
            decimalBalance = randomBigDecimal(),
            contract = randomAddress()
        )
        val owner = EthConverter.convert(erc20Balance.owner)
        val contract = EthConverter.convert(erc20Balance.contract)

        every { balanceControllerApi.getErc20Balance(contract, owner) } returns erc20Balance.toMono()

        val result = balanceService.getBalance(
            currencyId = contract,
            owner = owner
        )

        assertThat(result.balance).isEqualTo(erc20Balance.balance)
    }
}
