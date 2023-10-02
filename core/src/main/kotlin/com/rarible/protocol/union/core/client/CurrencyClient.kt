package com.rarible.protocol.union.core.client

import com.rarible.core.common.nowMillis
import com.rarible.core.logging.Logger
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.CurrencyDto
import com.rarible.protocol.union.core.converter.CurrencyConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CurrencyUsdRateDto
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class CurrencyClient(
    private val currencyControllerApi: CurrencyControllerApi
) {

    suspend fun fetchRate(blockchain: BlockchainDto, address: String, at: Instant?): CurrencyUsdRateDto? {
        val result = currencyControllerApi.getCurrencyRate(
            CurrencyConverter.convert(blockchain),
            address,
            (at ?: nowMillis()).toEpochMilli()
        ).awaitFirstOrNull()

        if (result == null) {
            logger.warn("Couldn't find rate for $blockchain:$address")
        }

        return result?.let { CurrencyConverter.convert(result) }
    }

    suspend fun getAllCurrencies(): List<CurrencyDto> {
        return currencyControllerApi.getAllCurrencies()
            .awaitFirstOrNull()?.currencies ?: emptyList()
    }

    companion object {
        val logger by Logger()
    }
}
