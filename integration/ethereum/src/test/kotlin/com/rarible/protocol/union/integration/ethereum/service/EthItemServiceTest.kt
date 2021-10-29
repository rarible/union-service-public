package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.NftItemsDto
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.integration.ethereum.data.randomAddressString
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

class EthItemServiceTest {

    private val itemControllerApi: NftItemControllerApi = mockk()
    private val service = EthItemService(BlockchainDto.ETHEREUM, itemControllerApi)

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
    fun `ethereum get item meta by id`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val meta = randomEthItemMeta()

        val expected = EthItemConverter.convert(meta)

        coEvery { itemControllerApi.getNftItemMetaById(itemId.value) } returns meta.toMono()

        val result = service.getItemMetaById(itemId.value)

        assertThat(result).isEqualTo(expected)
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
            itemControllerApi.getNftItemsByCollection(collection, continuation, size)
        } returns NftItemsDto(43, "abc", listOf(item)).toMono()

        val result = service.getItemsByCollection(collection, continuation, size)

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