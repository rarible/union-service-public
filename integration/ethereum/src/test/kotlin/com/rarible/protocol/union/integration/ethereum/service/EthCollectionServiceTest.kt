package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.NftCollectionsDto
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthCollectionConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.data.randomAddressString
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toMono

class EthCollectionServiceTest {

    private val collectionControllerApi: NftCollectionControllerApi = mockk()
    private val service = EthereumCollectionService(collectionControllerApi)

    @Test
    fun `ethereum get all collections`() = runBlocking<Unit> {
        val collection = randomEthCollectionDto()

        val continuation = randomString()
        val size = randomInt()

        val expected = EthCollectionConverter.convert(collection, BlockchainDto.ETHEREUM)

        coEvery {
            collectionControllerApi.searchNftAllCollections(continuation, size)
        } returns NftCollectionsDto(321, "abc", listOf(collection)).toMono()

        val result = service.getAllCollections(continuation, size)

        assertThat(result.total).isEqualTo(321)
        assertThat(result.continuation).isEqualTo("abc")
        assertThat(result.entities).hasSize(1)
        assertThat(result.entities[0]).isEqualTo(expected)
    }

    @Test
    fun `ethereum get collection by id`() = runBlocking<Unit> {
        val collectionId = randomAddressString()
        val collection = randomEthCollectionDto(EthConverter.convertToAddress(collectionId))

        val expected = EthCollectionConverter.convert(collection, BlockchainDto.ETHEREUM)

        coEvery {
            collectionControllerApi.getNftCollectionById(collectionId)
        } returns collection.toMono()

        val result = service.getCollectionById(collectionId)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `ethereum get collections by owner`() = runBlocking<Unit> {
        val owner = randomAddressString()
        val collection = randomEthCollectionDto()

        val continuation = randomString()
        val size = randomInt()

        val expected = EthCollectionConverter.convert(collection, BlockchainDto.ETHEREUM)

        coEvery {
            collectionControllerApi.searchNftCollectionsByOwner(owner, continuation, size)
        } returns NftCollectionsDto(100, "abc", listOf(collection)).toMono()

        val result = service.getCollectionsByOwner(
            owner,
            continuation,
            size
        )

        assertThat(result.total).isEqualTo(100)
        assertThat(result.continuation).isEqualTo("abc")
        assertThat(result.entities).hasSize(1)
        assertThat(result.entities[0]).isEqualTo(expected)
    }
}