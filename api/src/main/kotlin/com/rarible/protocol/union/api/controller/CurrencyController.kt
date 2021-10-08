package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CurrencyUsdRateDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class CurrencyController(
    private val currencyService: CurrencyService
) : CurrencyControllerApi {

    override suspend fun getCurrencyUsdRate(
        blockchain: BlockchainDto,
        address: String,
        at: Instant
    ): ResponseEntity<CurrencyUsdRateDto> {
        val result = currencyService.getRate(blockchain, address, at)
        return ResponseEntity.ok(result)
    }
}