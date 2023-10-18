package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.configuration.ApiProperties
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.exception.UnionValidationException
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderFeesDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@ExperimentalCoroutinesApi
@RestController
class OrderSettingsController(
    private val apiProperties: ApiProperties
) : OrderSettingsControllerApi {

    override suspend fun getOrderFees(blockchain: BlockchainDto?): ResponseEntity<OrderFeesDto> {
        if (blockchain == null) {
            throw UnionValidationException("Param 'blockchain' is required")
        } else {
            val fees = apiProperties.orderSettings.fees[blockchain] ?: throw UnionNotFoundException("Settings are not found")
            return ResponseEntity.ok(OrderFeesDto(fees))
        }
    }

}
