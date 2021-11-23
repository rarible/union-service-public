package com.rarible.protocol.union.api.controller.advice

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.core.exception.UnionDataFormatException
import com.rarible.protocol.union.core.exception.UnionException
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.dto.BlockchainIdFormatException
import com.rarible.protocol.union.dto.UnionApiErrorBadRequestDto
import com.rarible.protocol.union.dto.UnionApiErrorEntityNotFoundDto
import com.rarible.protocol.union.dto.UnionApiErrorServerErrorDto
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ServerWebInputException

@RestControllerAdvice(basePackages = ["com.rarible.protocol.union.api.controller"])
class ErrorsController {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(WebClientResponseProxyException::class)
    fun handleWebClientResponseException(ex: WebClientResponseProxyException) = mono {
        logger.warn("Exception during request to blockchain service: {}; response: {}", ex.message, ex.data)
        val convertedData = ErrorsConverter.convert(ex.data)
            ?: ErrorsConverter.getDefault(ex.statusCode, ex.message ?: "")
        ResponseEntity.status(ex.statusCode).body(convertedData)
    }

    @ExceptionHandler(UnionNotFoundException::class)
    fun handleUnionNotFoundException(ex: Exception) = mono {
        val error = UnionApiErrorEntityNotFoundDto(message = ex.message ?: "Entity not found")
        logger.warn(error.message)
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(error)
    }

    @ExceptionHandler(
        value = [
            ServerWebInputException::class,
            BlockchainIdFormatException::class,
            UnionException::class
        ]
    )
    fun handleServerWebInputException(ex: Exception) = mono {
        val error = UnionApiErrorBadRequestDto(
            code = UnionApiErrorBadRequestDto.Code.BAD_REQUEST,
            message = ex.cause?.cause?.message ?: ex.cause?.message ?: ex.message ?: ""
        )
        logger.warn("Web input error: {}", error.message)
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    @ExceptionHandler(UnionDataFormatException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun dataException(ex: Throwable) = mono {
        logger.error("Unexpected error during request: {}", ex.message)
        UnionApiErrorServerErrorDto(
            code = UnionApiErrorServerErrorDto.Code.UNKNOWN,
            // In order to do not expose conversion error details, we using here dedicated message,
            // details could be found in logs
            message = "Unexpected data error"
        )
    }

    @ExceptionHandler(Throwable::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleException(ex: Throwable) = mono {
        logger.error("Unexpected server error: {}", ex)
        UnionApiErrorServerErrorDto(
            code = UnionApiErrorServerErrorDto.Code.UNKNOWN,
            message = ex.message ?: "Unexpected server error"
        )
    }
}
