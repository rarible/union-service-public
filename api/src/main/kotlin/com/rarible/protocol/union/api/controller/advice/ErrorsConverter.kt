package com.rarible.protocol.union.api.controller.advice

import com.rarible.dipdup.client.exception.DipDupNotFound
import com.rarible.protocol.dto.EthereumApiErrorBadRequestDto
import com.rarible.protocol.dto.EthereumApiErrorEntityNotFoundDto
import com.rarible.protocol.dto.EthereumApiErrorServerErrorDto
import com.rarible.protocol.dto.EthereumOrderDataApiErrorDto
import com.rarible.protocol.dto.EthereumOrderUpdateApiErrorDto
import com.rarible.protocol.union.dto.UnionApiErrorBadRequestDto
import com.rarible.protocol.union.dto.UnionApiErrorEntityNotFoundDto
import com.rarible.protocol.union.dto.UnionApiErrorServerErrorDto
import com.rarible.tzkt.model.TzktBadRequest
import org.springframework.http.HttpStatus

object ErrorsConverter {

    fun getDefault(status: HttpStatus, message: String): Any {
        return when (status) {
            HttpStatus.BAD_REQUEST -> UnionApiErrorBadRequestDto(message = message)
            HttpStatus.NOT_FOUND -> UnionApiErrorEntityNotFoundDto(message = message)
            else -> UnionApiErrorServerErrorDto(message = message)
        }
    }

    fun convert(data: Any?): Any? {
        return when (data) {
            // ETHEREUM
            is EthereumApiErrorBadRequestDto -> UnionApiErrorBadRequestDto(convert(data.code), data.message)
            is EthereumOrderUpdateApiErrorDto -> UnionApiErrorBadRequestDto(convert(data.code), data.message)
            is EthereumOrderDataApiErrorDto -> UnionApiErrorBadRequestDto(convert(data.code), data.message)
            is EthereumApiErrorServerErrorDto -> UnionApiErrorServerErrorDto(message = data.message)
            is EthereumApiErrorEntityNotFoundDto -> UnionApiErrorEntityNotFoundDto(message = data.message)
            // FLOW
            // TEZOS
            is DipDupNotFound -> UnionApiErrorEntityNotFoundDto(message = data.message ?: "")
            is TzktBadRequest -> UnionApiErrorBadRequestDto(UnionApiErrorBadRequestDto.Code.BAD_REQUEST, data.message ?: "")
            else -> null
        }
    }

    private fun convert(code: EthereumApiErrorBadRequestDto.Code): UnionApiErrorBadRequestDto.Code {
        return when (code) {
            EthereumApiErrorBadRequestDto.Code.VALIDATION -> UnionApiErrorBadRequestDto.Code.VALIDATION
            EthereumApiErrorBadRequestDto.Code.BAD_REQUEST -> UnionApiErrorBadRequestDto.Code.BAD_REQUEST
        }
    }

    private fun convert(code: EthereumOrderUpdateApiErrorDto.Code): UnionApiErrorBadRequestDto.Code {
        return when (code) {
            EthereumOrderUpdateApiErrorDto.Code.INCORRECT_SIGNATURE,
            EthereumOrderUpdateApiErrorDto.Code.INCORRECT_ORDER_DATA,
            EthereumOrderUpdateApiErrorDto.Code.INCORRECT_PRICE,
            EthereumOrderUpdateApiErrorDto.Code.INCORRECT_LAZY_ASSET,
            EthereumOrderUpdateApiErrorDto.Code.ORDER_CANCELED,
            EthereumOrderUpdateApiErrorDto.Code.ORDER_INVALID_UPDATE -> UnionApiErrorBadRequestDto.Code.BAD_REQUEST
        }
    }

    private fun convert(code: EthereumOrderDataApiErrorDto.Code): UnionApiErrorBadRequestDto.Code {
        return when (code) {
            EthereumOrderDataApiErrorDto.Code.BAD_REQUEST,
            EthereumOrderDataApiErrorDto.Code.INCORRECT_ORDER_DATA -> UnionApiErrorBadRequestDto.Code.BAD_REQUEST

            EthereumOrderDataApiErrorDto.Code.VALIDATION -> UnionApiErrorBadRequestDto.Code.VALIDATION
        }
    }
}
