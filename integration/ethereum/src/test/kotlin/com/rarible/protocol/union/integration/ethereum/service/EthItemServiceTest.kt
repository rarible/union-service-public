package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.EthMetaStatusDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.NftItemRoyaltyDto
import com.rarible.protocol.dto.NftItemRoyaltyListDto
import com.rarible.protocol.dto.NftItemsDto
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.union.core.exception.UnionMetaException
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthMetaConverter
import com.rarible.protocol.union.integration.ethereum.data.randomAddressString
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@ExtendWith(MockKExtension::class)
class EthItemServiceTest {

    private val blockchain = BlockchainDto.ETHEREUM

    @MockK
    private lateinit var itemControllerApi: NftItemControllerApi

    @InjectMockKs
    private lateinit var service: EthItemService

    @Test
    fun `ethereum get all items`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val item = randomEthNftItemDto(itemId)

        val continuation = randomString()
        val size = randomInt()
        val lastUpdatedFrom = randomLong()
        val lastUpdatedTo = randomLong()

        val expected = EthItemConverter.convert(item, BlockchainDto.ETHEREUM)

        coEvery {
            itemControllerApi.getNftAllItems(continuation, size, true, lastUpdatedFrom, lastUpdatedTo)
        } returns NftItemsDto(500, "abc", listOf(item)).toMono()

        val result = service.getAllItems(continuation, size, true, lastUpdatedFrom, lastUpdatedTo)

        assertThat(result.total).isEqualTo(500)
        assertThat(result.continuation).isEqualTo("abc")
        assertThat(result.entities).hasSize(1)
        assertThat(result.entities[0]).isEqualTo(expected)
    }

    @Test
    fun `ethereum get item by id`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val item = randomEthNftItemDto(itemId)

        val expected = EthItemConverter.convert(item, BlockchainDto.ETHEREUM)

        coEvery { itemControllerApi.getNftItemById(item.id) } returns item.toMono()

        val result = service.getItemById(itemId.value)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `ethereum get item royalties by id`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val royalty = NftItemRoyaltyDto(randomAddress(), randomInt())
        val royaltyList = NftItemRoyaltyListDto(listOf(royalty))

        val expected = EthConverter.convert(royalty, itemId.blockchain)

        coEvery { itemControllerApi.getNftItemRoyaltyById(itemId.value) } returns royaltyList.toMono()

        val result = service.getItemRoyaltiesById(itemId.value)

        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo(expected)
    }

    @Test
    fun `ethereum get item meta by id`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val meta = randomEthItemMeta()

        val expected = EthMetaConverter.convert(meta, itemId.value)

        coEvery { itemControllerApi.getNftItemMetaById(itemId.value) } returns meta.toMono()

        val result = service.getItemMetaById(itemId.value)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `ethereum meta unparseable link`() = runBlocking {
        coEvery { itemControllerApi.getNftItemMetaById(any()) } returns Mono.just(
            NftItemMetaDto(
                name = "",
                status = EthMetaStatusDto.UNPARSEABLE_LINK
            )
        )

        val exception = assertThrows<UnionMetaException> {
            service.getItemMetaById("")
        }

        Assertions.assertEquals(UnionMetaException.ErrorCode.CORRUPTED_URL, exception.code)
    }

    @Test
    fun `ethereum meta unparseable json`() = runBlocking {
        coEvery { itemControllerApi.getNftItemMetaById(any()) } returns Mono.just(
            NftItemMetaDto(
                name = "",
                status = EthMetaStatusDto.UNPARSEABLE_JSON
            )
        )

        val exception = assertThrows<UnionMetaException> {
            service.getItemMetaById("")
        }

        Assertions.assertEquals(UnionMetaException.ErrorCode.CORRUPTED_DATA, exception.code)
    }

    @Test
    fun `ethereum meta timeout`() = runBlocking {
        coEvery { itemControllerApi.getNftItemMetaById(any()) } returns Mono.just(
            NftItemMetaDto(
                name = "",
                status = EthMetaStatusDto.TIMEOUT
            )
        )

        val exception = assertThrows<UnionMetaException> {
            service.getItemMetaById("")
        }

        Assertions.assertEquals(UnionMetaException.ErrorCode.TIMEOUT, exception.code)
    }

    @Test
    fun `ethereum meta not found`() = runBlocking<Unit> {
        coEvery { itemControllerApi.getNftItemMetaById(any()) } throws NftItemControllerApi.ErrorGetNftItemMetaById(
            WebClientResponseException(404, "", null, null, null)
        )

        assertThrows<UnionNotFoundException> {
            service.getItemMetaById("")
        }
    }

    @Test
    fun `ethereum meta unknown error`() = runBlocking<Unit> {
        coEvery { itemControllerApi.getNftItemMetaById(any()) } returns Mono.just(
            NftItemMetaDto(
                name = "",
                status = EthMetaStatusDto.ERROR
            )
        )

        val exception = assertThrows<UnionMetaException> {
            service.getItemMetaById("")
        }

        Assertions.assertEquals(UnionMetaException.ErrorCode.ERROR, exception.code)
    }

    @Test
    fun `ethereum reset item meta`() = runBlocking<Unit> {
        val itemId = randomEthItemId()

        coEvery { itemControllerApi.resetNftItemMetaById(itemId.value) } returns Mono.empty()

        service.resetItemMeta(itemId.value)

        coVerify(exactly = 1) { itemControllerApi.resetNftItemMetaById(itemId.value) }
    }

    @Test
    fun `ethereum get items by collection`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val item = randomEthNftItemDto(itemId)

        val continuation = randomString()
        val size = randomInt()
        val collection = randomAddressString()

        val expected = EthItemConverter.convert(item, BlockchainDto.ETHEREUM)

        coEvery {
            itemControllerApi.getNftItemsByCollection(collection, any(), continuation, size)
        } returns NftItemsDto(43, "abc", listOf(item)).toMono()

        val result = service.getItemsByCollection(collection, null, continuation, size)

        assertThat(result.total).isEqualTo(43)
        assertThat(result.continuation).isEqualTo("abc")
        assertThat(result.entities).hasSize(1)
        assertThat(result.entities[0]).isEqualTo(expected)
    }

    @Test
    fun `ethereum get items by creator`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val item = randomEthNftItemDto(itemId)

        val continuation = randomString()
        val size = randomInt()
        val creator = randomAddressString()

        val expected = EthItemConverter.convert(item, BlockchainDto.ETHEREUM)

        coEvery {
            itemControllerApi.getNftItemsByCreator(creator, continuation, size)
        } returns NftItemsDto(31, "abc", listOf(item)).toMono()

        val result = service.getItemsByCreator(creator, continuation, size)

        assertThat(result.total).isEqualTo(31)
        assertThat(result.continuation).isEqualTo("abc")
        assertThat(result.entities).hasSize(1)
        assertThat(result.entities[0]).isEqualTo(expected)
    }

    @Test
    fun `ethereum get items by owner`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val item = randomEthNftItemDto(itemId)

        val continuation = randomString()
        val size = randomInt()
        val owner = randomAddressString()

        val expected = EthItemConverter.convert(item, BlockchainDto.ETHEREUM)

        coEvery {
            itemControllerApi.getNftItemsByOwner(owner, continuation, size)
        } returns NftItemsDto(55, "abc", listOf(item)).toMono()

        val result = service.getItemsByOwner(owner, continuation, size)

        assertThat(result.total).isEqualTo(55)
        assertThat(result.continuation).isEqualTo("abc")
        assertThat(result.entities).hasSize(1)
        assertThat(result.entities[0]).isEqualTo(expected)
    }
}
