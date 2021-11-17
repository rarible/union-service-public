package com.rarible.protocol.union.api.controller.advice

import com.rarible.protocol.dto.EthereumApiErrorBadRequestDto
import com.rarible.protocol.dto.EthereumApiErrorEntityNotFoundDto
import com.rarible.protocol.dto.EthereumApiErrorServerErrorDto
import com.rarible.protocol.tezos.dto.BadRequestDto
import com.rarible.protocol.tezos.dto.EntityNotFoundDto
import com.rarible.protocol.tezos.dto.ServerErrorDto
import com.rarible.protocol.union.dto.UnionApiErrorBadRequestDto
import com.rarible.protocol.union.dto.UnionApiErrorEntityNotFoundDto
import com.rarible.protocol.union.dto.UnionApiErrorServerErrorDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class ErrorsConverterTest {

    @Test
    fun `get default`() {
        val notFound = ErrorsConverter.getDefault(HttpStatus.NOT_FOUND, "333")
        val badRequest = ErrorsConverter.getDefault(HttpStatus.BAD_REQUEST, "555")
        val other = ErrorsConverter.getDefault(HttpStatus.CONFLICT, "777")

        assertThat(notFound).isInstanceOf(UnionApiErrorEntityNotFoundDto::class.java)
        assertThat((notFound as UnionApiErrorEntityNotFoundDto).message).isEqualTo("333")

        assertThat(badRequest).isInstanceOf(UnionApiErrorBadRequestDto::class.java)
        assertThat((badRequest as UnionApiErrorBadRequestDto).message).isEqualTo("555")

        assertThat(other).isInstanceOf(UnionApiErrorServerErrorDto::class.java)
        assertThat((other as UnionApiErrorServerErrorDto).message).isEqualTo("777")
    }

    @Test
    fun `convert eth`() {
        val notFound = ErrorsConverter.convert(
            EthereumApiErrorEntityNotFoundDto(code = EthereumApiErrorEntityNotFoundDto.Code.NOT_FOUND, message = "333")
        )

        val badRequest = ErrorsConverter.convert(
            EthereumApiErrorBadRequestDto(code = EthereumApiErrorBadRequestDto.Code.VALIDATION, message = "222")
        )

        val serverError = ErrorsConverter.convert(
            EthereumApiErrorServerErrorDto(code = EthereumApiErrorServerErrorDto.Code.UNKNOWN, message = "666")
        )

        val unsupported = ErrorsConverter.convert("wrongtype")

        assertThat(notFound).isInstanceOf(UnionApiErrorEntityNotFoundDto::class.java)
        assertThat((notFound as UnionApiErrorEntityNotFoundDto).message).isEqualTo("333")

        assertThat(badRequest).isInstanceOf(UnionApiErrorBadRequestDto::class.java)
        assertThat((badRequest as UnionApiErrorBadRequestDto).message).isEqualTo("222")

        assertThat(serverError).isInstanceOf(UnionApiErrorServerErrorDto::class.java)
        assertThat((serverError as UnionApiErrorServerErrorDto).message).isEqualTo("666")

        assertThat(unsupported).isNull()
        assertThat(ErrorsConverter.convert(null)).isNull()
    }

    @Test
    fun `convert tezos`() {
        val notFound = ErrorsConverter.convert(
            EntityNotFoundDto(code = EntityNotFoundDto.Code.NOT_FOUND, message = "333")
        )

        val badRequest = ErrorsConverter.convert(
            BadRequestDto(code = BadRequestDto.Code.VALIDATION, message = "222")
        )

        val serverError = ErrorsConverter.convert(
            ServerErrorDto(code = ServerErrorDto.Code.UNEXPECTED_API_ERROR, message = "666")
        )

        val unsupported = ErrorsConverter.convert("wrongtype")

        assertThat(notFound).isInstanceOf(UnionApiErrorEntityNotFoundDto::class.java)
        assertThat((notFound as UnionApiErrorEntityNotFoundDto).message).isEqualTo("333")

        assertThat(badRequest).isInstanceOf(UnionApiErrorBadRequestDto::class.java)
        assertThat((badRequest as UnionApiErrorBadRequestDto).message).isEqualTo("222")

        assertThat(serverError).isInstanceOf(UnionApiErrorServerErrorDto::class.java)
        assertThat((serverError as UnionApiErrorServerErrorDto).message).isEqualTo("666")

        assertThat(unsupported).isNull()
        assertThat(ErrorsConverter.convert(null)).isNull()
    }
}