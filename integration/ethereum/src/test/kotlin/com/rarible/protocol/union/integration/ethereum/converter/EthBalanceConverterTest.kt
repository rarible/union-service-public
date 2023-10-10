package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.protocol.dto.Erc20DecimalBalanceDto
import com.rarible.protocol.dto.EthBalanceDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CurrencyIdDto
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address

class EthBalanceConverterTest {

    private val blockchain = BlockchainDto.ETHEREUM

    @Test
    fun `convert - ok, native currency`() = runBlocking<Unit> {
        val ethBalance = EthBalanceDto(
            owner = randomAddress(),
            balance = randomBigInt(),
            decimalBalance = randomBigDecimal()
        )

        val result = EthBalanceConverter.convert(ethBalance, blockchain)

        assertThat(result.balance).isEqualTo(ethBalance.balance)
        assertThat(result.decimal).isEqualTo(ethBalance.decimalBalance)
        assertThat(result.owner).isEqualTo(UnionAddressConverter.convert(blockchain, ethBalance.owner.prefixed()))
        assertThat(result.currencyId).isEqualTo(CurrencyIdDto(blockchain, Address.ZERO().prefixed(), null))
    }

    @Test
    fun `convert - ok, erc20 currency`() = runBlocking<Unit> {
        val ethBalance = Erc20DecimalBalanceDto(
            owner = randomAddress(),
            balance = randomBigInt(),
            decimalBalance = randomBigDecimal(),
            contract = randomAddress()
        )

        val result = EthBalanceConverter.convert(ethBalance, blockchain)

        assertThat(result.balance).isEqualTo(ethBalance.balance)
        assertThat(result.decimal).isEqualTo(ethBalance.decimalBalance)
        assertThat(result.owner).isEqualTo(UnionAddressConverter.convert(blockchain, ethBalance.owner.prefixed()))
        assertThat(result.currencyId).isEqualTo(CurrencyIdDto(blockchain, ethBalance.contract.prefixed(), null))
    }
}
