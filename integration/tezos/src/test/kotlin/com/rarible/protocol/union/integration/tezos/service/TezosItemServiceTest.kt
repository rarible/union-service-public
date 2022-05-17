package com.rarible.protocol.union.integration.tezos.service

import com.rarible.protocol.tezos.api.client.NftItemControllerApi
import com.rarible.protocol.tezos.dto.NftItemRoyaltiesDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosItemId
import com.rarible.protocol.union.integration.tezos.data.randomTezosPartDto
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemService
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.kotlin.core.publisher.toMono

class TezosItemServiceTest {

    private val itemControllerApi: NftItemControllerApi = mockk()
    private val service = TezosItemService(itemControllerApi, object: TzktItemService {})

    @BeforeEach
    fun beforeEach() {
        clearMocks(itemControllerApi)
    }

    @Test
    fun `get item royalties`() = runBlocking<Unit> {
        val itemId = randomTezosItemId()
        val r1 = randomTezosPartDto()
        val r2 = randomTezosPartDto()

        coEvery {
            itemControllerApi.getNftItemRoyalties(itemId.value)
        } returns NftItemRoyaltiesDto(listOf(r1, r2)).toMono()

        val royalties = service.getItemRoyaltiesById(itemId.value)

        assertThat(royalties).hasSize(2)
        assertThat(royalties[0].value).isEqualTo(r1.value)
        assertThat(royalties[1].value).isEqualTo(r2.value)
    }

    @Test
    fun `get item royalties - not found`() = runBlocking<Unit> {
        val itemId = randomTezosItemId()

        coEvery {
            itemControllerApi.getNftItemRoyalties(itemId.value)
        } throws WebClientResponseException(404, "", null, null, null)

        val royalties = service.getItemRoyaltiesById(itemId.value)

        assertThat(royalties).hasSize(0)
    }

    @Test
    fun `get item royalties - unexpected status code`() = runBlocking<Unit> {
        val itemId = randomTezosItemId()

        coEvery {
            itemControllerApi.getNftItemRoyalties(itemId.value)
        } throws WebClientResponseException(500, "", null, null, null)

        assertThrows<WebClientResponseException> {
            service.getItemRoyaltiesById(itemId.value)
        }
    }
}
