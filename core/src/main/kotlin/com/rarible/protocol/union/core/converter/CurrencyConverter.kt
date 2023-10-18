package com.rarible.protocol.union.core.converter

import com.rarible.protocol.currency.dto.CurrencyDto
import com.rarible.protocol.currency.dto.CurrencyRateDto
import com.rarible.protocol.union.core.exception.UnionCurrencyException
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CurrencyIdDto
import com.rarible.protocol.union.dto.CurrencyUsdRateDto
import java.math.BigInteger

object CurrencyConverter {

    fun convert(source: CurrencyRateDto): CurrencyUsdRateDto {
        return CurrencyUsdRateDto(
            currencyId = source.fromCurrencyId,
            symbol = source.fromCurrencyId,
            rate = source.rate,
            date = source.date
        )
    }

    fun convert(source: CurrencyDto): com.rarible.protocol.union.dto.CurrencyDto {
        val pair = source.address.split(":")
        val address = pair[0]
        // Some currencies have tokenId (in Tezos, for example)
        val tokenId = pair.getOrNull(1)?.let { BigInteger(it) }
        return com.rarible.protocol.union.dto.CurrencyDto(
            currencyId = CurrencyIdDto(convert(source.blockchain), address, tokenId),
            symbol = source.currencyId,
            alias = source.alias
        )
    }

    fun convert(blockchain: BlockchainDto): String {
        return blockchain.name
    }

    fun convert(blockchain: String): BlockchainDto {
        return BlockchainDto.values().find { it.name == blockchain }
            ?: throw UnionCurrencyException("Unsupported blockchain: $blockchain")
    }
}
