package com.rarible.protocol.union.api.controller

import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.protocol.dto.Erc20DecimalBalanceDto
import com.rarible.protocol.union.api.client.BalanceControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.CurrencyIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.integration.ethereum.data.randomEthAddress
import io.mockk.every
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address

@IntegrationTest
class BalanceControllerIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var balanceControllerClient: BalanceControllerApi

    @Test
    fun `get balance - ok`() = runBlocking<Unit> {
        val currencyId = CurrencyIdDto(BlockchainDto.ETHEREUM, randomEthAddress(), null)
        val owner = UnionAddress(BlockchainGroupDto.ETHEREUM, randomEthAddress())
        val ethBalance = Erc20DecimalBalanceDto(
            owner = Address.apply(owner.value),
            balance = randomBigInt(),
            decimalBalance = randomBigDecimal(2, 2),
            contract = Address.apply(currencyId.value)
        )

        every { testEthereumBalanceApi.getErc20Balance(currencyId.value, owner.value) } returns ethBalance.toMono()

        val result = balanceControllerClient.getBalance(currencyId.fullId(), owner.fullId()).awaitSingle()

        assertThat(result.balance).isEqualTo(ethBalance.balance)
        assertThat(result.decimal).isEqualTo(ethBalance.decimalBalance)
        assertThat(result.owner).isEqualTo(owner)
        assertThat(result.currencyId).isEqualTo(currencyId)
    }
}
