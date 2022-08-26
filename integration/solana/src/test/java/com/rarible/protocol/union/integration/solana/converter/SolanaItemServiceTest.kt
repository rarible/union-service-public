package com.rarible.protocol.union.integration.solana.converter

import com.rarible.protocol.solana.api.client.TokenControllerApi
import com.rarible.protocol.solana.dto.SolanaApiMetaErrorDto
import com.rarible.protocol.union.core.exception.UnionMetaException
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.integration.solana.service.SolanaItemService
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClientResponseException

class SolanaItemServiceTest {
    private val tokenApi = mockk<TokenControllerApi>()
    private val solanaItemService = SolanaItemService(tokenApi)

    @BeforeEach
    fun beforeEach() {
        clearMocks(tokenApi)
    }

    @Test
    fun `Meta unparseable link`() = runBlocking {
        coEvery { tokenApi.getTokenMetaByAddress(any()) } throws TokenControllerApi.ErrorGetTokenMetaByAddress(
            WebClientResponseException(500, "", null, null, null)
        ).apply {
            on500 = SolanaApiMetaErrorDto(
                code = SolanaApiMetaErrorDto.Code.UNPARSEABLE_LINK,
                message = "Unparseable link"
            )
        }

        val exception = assertThrows<UnionMetaException> {
            solanaItemService.getItemMetaById("")
        }

        Assertions.assertEquals(UnionMetaException.ErrorCode.UNPARSEABLE_LINK, exception.code)
    }

    @Test
    fun `Meta unparseable json`() = runBlocking {
        coEvery { tokenApi.getTokenMetaByAddress(any()) } throws TokenControllerApi.ErrorGetTokenMetaByAddress(
            WebClientResponseException(500, "", null, null, null)
        ).apply {
            on500 = SolanaApiMetaErrorDto(
                code = SolanaApiMetaErrorDto.Code.UNPARSEABLE_JSON,
                message = "Unparseable json"
            )
        }

        val exception = assertThrows<UnionMetaException> {
            solanaItemService.getItemMetaById("")
        }

        Assertions.assertEquals(UnionMetaException.ErrorCode.UNPARSEABLE_JSON, exception.code)
    }

    @Test
    fun `Meta timeout`() = runBlocking {
        coEvery { tokenApi.getTokenMetaByAddress(any()) } throws TokenControllerApi.ErrorGetTokenMetaByAddress(
            WebClientResponseException(500, "", null, null, null)
        ).apply {
            on500 = SolanaApiMetaErrorDto(
                code = SolanaApiMetaErrorDto.Code.TIMEOUT,
                message = "Timeout"
            )
        }

        val exception = assertThrows<UnionMetaException> {
            solanaItemService.getItemMetaById("")
        }

        Assertions.assertEquals(UnionMetaException.ErrorCode.TIMEOUT, exception.code)
    }

    @Test
    fun `Meta not found`() = runBlocking<Unit> {
        coEvery { tokenApi.getTokenMetaByAddress(any()) } throws TokenControllerApi.ErrorGetTokenMetaByAddress(
            WebClientResponseException(404, "", null, null, null)
        )

        assertThrows<UnionNotFoundException> {
            solanaItemService.getItemMetaById("")
        }
    }

    @Test
    fun `Meta unknown error`() = runBlocking<Unit> {
        coEvery { tokenApi.getTokenMetaByAddress(any()) } throws TokenControllerApi.ErrorGetTokenMetaByAddress(
            WebClientResponseException(500, "", null, null, null)
        ).apply {
            on500 = SolanaApiMetaErrorDto(
                code = SolanaApiMetaErrorDto.Code.ERROR,
                message = "Timeout"
            )
        }

        val exception = assertThrows<UnionMetaException> {
            solanaItemService.getItemMetaById("")
        }

        Assertions.assertEquals(UnionMetaException.ErrorCode.UNKNOWN, exception.code)
    }
}