package com.rarible.protocol.union.integration.flow.service

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.FlowNftOwnershipsDto
import com.rarible.protocol.dto.NftOwnershipsByIdRequestDto
import com.rarible.protocol.flow.nft.api.client.FlowNftOwnershipControllerApi
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.flow.converter.FlowOwnershipConverter
import com.rarible.protocol.union.integration.flow.data.randomFlowNftOwnershipDto
import com.rarible.protocol.union.integration.flow.data.randomFlowOwnershipId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import reactor.kotlin.core.publisher.toMono

@ExtendWith(MockKExtension::class)
internal class FlowOwnershipServiceTest {

    @MockK
    private lateinit var ownershipControllerApi: FlowNftOwnershipControllerApi

    @InjectMockKs
    private lateinit var service: FlowOwnershipService

    @Test
    fun `should get ownerships by ids`() = runBlocking<Unit> {
        // given
        val id1 = randomString()
        val id2 = randomString()
        val ownership1 = randomFlowNftOwnershipDto()
        val ownership2 = randomFlowNftOwnershipDto()
        val expected1 = FlowOwnershipConverter.convert(ownership1, BlockchainDto.FLOW)
        val expected2 = FlowOwnershipConverter.convert(ownership2, BlockchainDto.FLOW)
        coEvery {
            ownershipControllerApi.getNftOwnershipsById(any())
        } returns FlowNftOwnershipsDto(null, listOf(ownership1, ownership2)).toMono()

        // when
        val actual = service.getOwnershipsByIds(listOf(id1, id2))

        // then
        assertThat(actual).containsExactly(expected1, expected2)
        coVerify {
            ownershipControllerApi.getNftOwnershipsById(NftOwnershipsByIdRequestDto(listOf(id1, id2)))
        }
        confirmVerified(ownershipControllerApi)
    }

    @Test
    fun `should get all ownerships`() = runBlocking<Unit> {
        // given
        val continuation = randomString()
        val size = 42
        val newContinuation = randomString()
        val id = randomFlowOwnershipId()
        val ownership = randomFlowNftOwnershipDto(id)
        coEvery {
            ownershipControllerApi.getNftAllOwnerships(any(), any())
        } returns FlowNftOwnershipsDto(newContinuation, listOf(ownership)).toMono()

        // when
        val actual = service.getOwnershipsAll(continuation, size)

        // then
        assertThat(actual.continuation).isEqualTo(newContinuation)
        assertThat(actual.entities).hasSize(1)
        assertThat(actual.entities.first().id.value).isEqualTo(id.value)
        coVerify {
            ownershipControllerApi.getNftAllOwnerships(continuation, size)
        }
        confirmVerified(ownershipControllerApi)
    }
}