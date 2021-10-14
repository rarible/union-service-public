package com.rarible.protocol.union.core.converter

import com.rarible.protocol.currency.dto.CurrencyRateDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CurrencyUsdRateDto

object CurrencyConverter {

    fun convert(source: CurrencyRateDto): CurrencyUsdRateDto {
        return CurrencyUsdRateDto(
            currencyId = source.fromCurrencyId,
            rate = source.rate,
            date = source.date
        )
    }

    fun convert(blockchain: BlockchainDto): com.rarible.protocol.currency.dto.BlockchainDto {
        return when (blockchain) {
            BlockchainDto.ETHEREUM -> com.rarible.protocol.currency.dto.BlockchainDto.ETHEREUM
            BlockchainDto.FLOW -> com.rarible.protocol.currency.dto.BlockchainDto.FLOW
            BlockchainDto.POLYGON -> com.rarible.protocol.currency.dto.BlockchainDto.POLYGON
            BlockchainDto.TEZOS -> com.rarible.protocol.currency.dto.BlockchainDto.TEZOS
        }
    }

}