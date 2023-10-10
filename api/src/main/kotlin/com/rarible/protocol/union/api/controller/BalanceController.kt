package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.core.service.BalanceService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BalanceDto
import com.rarible.protocol.union.dto.parser.CurrencyIdParser
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.converter.BalanceDtoConverter
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class BalanceController(
    private val router: BlockchainRouter<BalanceService>
) : BalanceControllerApi {

    override suspend fun getBalance(currencyId: String, owner: String): ResponseEntity<BalanceDto> {
        val fullCurrencyId = CurrencyIdParser.parse(currencyId)
        val ownerAddress = IdParser.parseAddress(owner)
        val result = router.getService(fullCurrencyId.blockchain)
            .getBalance(fullCurrencyId.value, ownerAddress.value)
        val dto = BalanceDtoConverter.convert(result)
        return ResponseEntity.ok(dto)
    }
}
