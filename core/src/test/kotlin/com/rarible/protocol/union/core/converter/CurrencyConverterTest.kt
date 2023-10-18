package com.rarible.protocol.union.core.converter

import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CurrencyIdDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CurrencyConverterTest {

    @Test
    fun `currency - regular`() {
        val currency = com.rarible.protocol.currency.dto.CurrencyDto(
            currencyId = "wusd",
            address = randomString(),
            blockchain = BlockchainDto.ETHEREUM.name,
            alias = "usd"
        )

        val result = CurrencyConverter.convert(currency)

        assertThat(result.currencyId).isEqualTo(CurrencyIdDto(BlockchainDto.ETHEREUM, currency.address, null))
        assertThat(result.alias).isEqualTo(currency.alias)
        assertThat(result.symbol).isEqualTo(currency.currencyId)
    }

    @Test
    fun `currency - with tokenId`() {
        val address = randomString()
        val tokenId = randomBigInt()
        val currency = com.rarible.protocol.currency.dto.CurrencyDto(
            currencyId = "tzx",
            address = "$address:$tokenId",
            blockchain = BlockchainDto.TEZOS.name,
            alias = null
        )

        val result = CurrencyConverter.convert(currency)

        assertThat(result.currencyId).isEqualTo(CurrencyIdDto(BlockchainDto.TEZOS, address, tokenId))
        assertThat(result.alias).isNull()
        assertThat(result.symbol).isEqualTo(currency.currencyId)
    }
}
