package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.NftOwnershipIdsDto
import com.rarible.protocol.dto.NftOwnershipsDto
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthOwnershipConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toMono

class EthOwnershipServiceTest {

    private val ownershipControllerApi: NftOwnershipControllerApi = mockk()
    private val service = EthereumOwnershipService(ownershipControllerApi)

    @Test
    fun `ethereum get ownership by id`() = runBlocking<Unit> {
        val ownershipId = randomEthOwnershipId()
        val ownership = randomEthOwnershipDto(ownershipId)

        val expected = EthOwnershipConverter.convert(ownership, BlockchainDto.ETHEREUM)

        coEvery { ownershipControllerApi.getNftOwnershipById(ownership.id, false) } returns ownership.toMono()

        val result = service.getOwnershipById(ownershipId.value)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `ethereum get ownerships by item`() = runBlocking<Unit> {
        val ownershipId = randomEthOwnershipId()
        val (contract, tokenId) = CompositeItemIdParser.split(ownershipId.itemIdValue)
        val ownership = randomEthOwnershipDto(ownershipId)

        val continuation = randomString()
        val size = randomInt()

        val expected = EthOwnershipConverter.convert(ownership, BlockchainDto.ETHEREUM)

        coEvery {
            ownershipControllerApi.getNftOwnershipsByItem(
                contract,
                tokenId.toString(),
                continuation,
                size
            )
        } returns NftOwnershipsDto(100, "abc", listOf(ownership)).toMono()

        val result = service.getOwnershipsByItem(
            ownershipId.itemIdValue,
            continuation,
            size
        )

        assertThat(result.total).isEqualTo(100)
        assertThat(result.continuation).isEqualTo("abc")
        assertThat(result.entities).hasSize(1)
        assertThat(result.entities[0]).isEqualTo(expected)
    }

    @Test
    fun `should get all ownerships`() = runBlocking<Unit> {
        // given
        val continuation = randomString()
        val size = 42
        val newContinuation = randomString()
        val id = randomEthOwnershipId()
        val ethOwnership = randomEthOwnershipDto(id)
        coEvery {
            ownershipControllerApi.getNftAllOwnerships(any(), any(), any())
        } returns NftOwnershipsDto(100, newContinuation, listOf(ethOwnership)).toMono()

        // when
        val actual = service.getOwnershipsAll(continuation, size)

        // then
        assertThat(actual.continuation).isEqualTo(newContinuation)
        assertThat(actual.entities).hasSize(1)
        assertThat(actual.entities.first().id.value).isEqualTo(id.value)
        coVerify {
            ownershipControllerApi.getNftAllOwnerships(continuation, size, false)
        }
        confirmVerified(ownershipControllerApi)
    }

    @Test
    fun `should get ownerships by ids`() = runBlocking<Unit> {
        // given
        val id1 = randomString()
        val id2 = randomString()
        val ownership1 = randomEthOwnershipDto()
        val ownership2 = randomEthOwnershipDto()
        val expected1 = EthOwnershipConverter.convert(ownership1, BlockchainDto.ETHEREUM)
        val expected2 = EthOwnershipConverter.convert(ownership2, BlockchainDto.ETHEREUM)
        coEvery {
            ownershipControllerApi.getNftOwnershipsByIds(any())
        } returns NftOwnershipsDto(2, null, listOf(ownership1, ownership2)).toMono()

        // when
        val actual = service.getOwnershipsByIds(listOf(id1, id2))

        // then
        assertThat(actual).containsExactly(expected1, expected2)
        coVerify {
            ownershipControllerApi.getNftOwnershipsByIds(NftOwnershipIdsDto(listOf(id1, id2)))
        }
        confirmVerified(ownershipControllerApi)
    }
}
