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

    fun convert(blockchain: BlockchainDto): com.rarible.protocol.currency.dto.BlockchainDto {
        return when (blockchain) {
            ETHEREUM -> com.rarible.protocol.currency.dto.BlockchainDto.ETHEREUM
            IMMUTABLEX -> com.rarible.protocol.currency.dto.BlockchainDto.IMMUTABLEX
            FLOW -> com.rarible.protocol.currency.dto.BlockchainDto.FLOW
            POLYGON -> com.rarible.protocol.currency.dto.BlockchainDto.POLYGON
            TEZOS -> com.rarible.protocol.currency.dto.BlockchainDto.TEZOS
            SOLANA -> com.rarible.protocol.currency.dto.BlockchainDto.SOLANA
            MANTLE -> com.rarible.protocol.currency.dto.BlockchainDto.MANTLE
        }
    }

    fun convert(blockchain: com.rarible.protocol.currency.dto.BlockchainDto): BlockchainDto {
        return when (blockchain) {
            com.rarible.protocol.currency.dto.BlockchainDto.ETHEREUM -> ETHEREUM
            com.rarible.protocol.currency.dto.BlockchainDto.FLOW -> FLOW
            com.rarible.protocol.currency.dto.BlockchainDto.POLYGON -> POLYGON
            com.rarible.protocol.currency.dto.BlockchainDto.TEZOS -> TEZOS
            com.rarible.protocol.currency.dto.BlockchainDto.SOLANA -> SOLANA
            com.rarible.protocol.currency.dto.BlockchainDto.IMMUTABLEX -> IMMUTABLEX
            com.rarible.protocol.currency.dto.BlockchainDto.MANTLE -> MANTLE
            com.rarible.protocol.currency.dto.BlockchainDto.OPTIMISM,
            com.rarible.protocol.currency.dto.BlockchainDto.APTOS -> throw UnionCurrencyException("Unsupported blockchain: $blockchain")
        }
    }
}
