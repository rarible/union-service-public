package com.rarible.protocol.union.core.service

import com.rarible.core.common.nowMillis
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.union.core.converter.CurrencyConverter
import com.rarible.protocol.union.core.exception.UnionBadRequestException
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CurrencyUsdRateDto
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class CurrencyService(
    private val currencyControllerApi: CurrencyControllerApi
) {

    suspend fun getCurrencyRate(blockchain: BlockchainDto, address: String, at: Instant?): CurrencyUsdRateDto {
        val result = currencyControllerApi.getCurrencyRate(
            convert(blockchain),
            address,
            (at ?: nowMillis()).toEpochMilli()
        ).awaitFirstOrNull() ?: throw UnionBadRequestException("Currency for {} with id [{}] is not supported")

        return CurrencyConverter.convert(result)
    }

    private fun convert(blockchain: BlockchainDto): com.rarible.protocol.currency.dto.BlockchainDto {
        return when (blockchain) {
            BlockchainDto.ETHEREUM -> com.rarible.protocol.currency.dto.BlockchainDto.ETHEREUM
            BlockchainDto.FLOW -> com.rarible.protocol.currency.dto.BlockchainDto.FLOW
            BlockchainDto.POLYGON -> com.rarible.protocol.currency.dto.BlockchainDto.POLYGON
        }
    }

}