package com.rarible.protocol.union.integration.solana.converter

import com.rarible.protocol.solana.api.client.TokenControllerApi
import com.rarible.protocol.solana.dto.TokenMetaDto
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
import reactor.core.publisher.Mono

class SolanaItemServiceTest {
    private val tokenApi = mockk<TokenControllerApi>()
    private val solanaItemService = SolanaItemService(tokenApi)

    @BeforeEach
    fun beforeEach() {
        clearMocks(tokenApi)
    }

    @Test
    fun `Meta unparseable link`() = runBlocking {
        coEvery { tokenApi.getTokenMetaByAddress(any()) } returns Mono.just(
            TokenMetaDto(
                name = "",
                status = TokenMetaDto.Status.UNPARSEABLE_LINK
            )
        )

        val exception = assertThrows<UnionMetaException> {
            solanaItemService.getItemMetaById("")
        }

        Assertions.assertEquals(UnionMetaException.ErrorCode.CORRUPTED_URL, exception.code)
    }

    @Test
    fun `Meta unparseable json`() = runBlocking {
        coEvery { tokenApi.getTokenMetaByAddress(any()) } returns Mono.just(
            TokenMetaDto(
                name = "",
                status = TokenMetaDto.Status.UNPARSEABLE_JSON
            )
        )

        val exception = assertThrows<UnionMetaException> {
            solanaItemService.getItemMetaById("")
        }

        Assertions.assertEquals(UnionMetaException.ErrorCode.CORRUPTED_DATA, exception.code)
    }

    @Test
    fun `Meta timeout`() = runBlocking {
        coEvery { tokenApi.getTokenMetaByAddress(any()) } returns Mono.just(
            TokenMetaDto(
                name = "",
                status = TokenMetaDto.Status.TIMEOUT
            )
        )

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
        coEvery { tokenApi.getTokenMetaByAddress(any()) } returns Mono.just(
            TokenMetaDto(
                name = "",
                status = TokenMetaDto.Status.ERROR
            )
        )

        val exception = assertThrows<UnionMetaException> {
            solanaItemService.getItemMetaById("")
        }

        Assertions.assertEquals(UnionMetaException.ErrorCode.ERROR, exception.code)
    }
}