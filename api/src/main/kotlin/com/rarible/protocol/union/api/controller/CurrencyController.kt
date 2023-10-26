package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.CurrenciesDto
import com.rarible.protocol.union.dto.CurrencyUsdRateDto
import com.rarible.protocol.union.dto.parser.CurrencyIdParser
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class CurrencyController(
    private val currencyService: CurrencyService
) : CurrencyControllerApi {

    override suspend fun getUsdRate(
        currencyId: String,
        at: Instant
    ): ResponseEntity<CurrencyUsdRateDto> {
        val parsedCurrencyId = CurrencyIdParser.parse(currencyId)
        val result = currencyService.getRate(parsedCurrencyId.blockchain, parsedCurrencyId.value, at)
        return ResponseEntity.ok(result)
    }

    override suspend fun getAllCurrencies(): ResponseEntity<CurrenciesDto> {
        val currencies = currencyService.getAllCurrencies()
        return ResponseEntity.ok(CurrenciesDto(currencies))
    }
}
