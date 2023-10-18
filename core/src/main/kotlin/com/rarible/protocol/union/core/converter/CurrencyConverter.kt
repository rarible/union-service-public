package com.rarible.protocol.union.core.converter

import com.rarible.protocol.currency.dto.CurrencyDto
import com.rarible.protocol.currency.dto.CurrencyRateDto
import com.rarible.protocol.union.core.exception.UnionCurrencyException
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainDto.ETHEREUM
import com.rarible.protocol.union.dto.BlockchainDto.FLOW
import com.rarible.protocol.union.dto.BlockchainDto.IMMUTABLEX
import com.rarible.protocol.union.dto.BlockchainDto.MANTLE
import com.rarible.protocol.union.dto.BlockchainDto.POLYGON
import com.rarible.protocol.union.dto.BlockchainDto.SOLANA
import com.rarible.protocol.union.dto.BlockchainDto.TEZOS
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
        return when (blockchain) {
            "ETHEREUM" -> ETHEREUM
            "FLOW" -> FLOW
            "POLYGON" -> POLYGON
            "TEZOS" -> TEZOS
            "SOLANA" -> SOLANA
            "IMMUTABLEX" -> IMMUTABLEX
            "MANTLE" -> MANTLE
            else -> throw UnionCurrencyException("Unsupported blockchain: $blockchain")
        }
    }
}
