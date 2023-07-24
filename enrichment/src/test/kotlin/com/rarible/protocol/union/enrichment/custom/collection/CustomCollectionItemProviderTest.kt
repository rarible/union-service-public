package com.rarible.protocol.union.enrichment.custom.collection

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomPolygonItemId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class CustomCollectionItemProviderTest {

    @MockK
    lateinit var ethService: ItemService

    @MockK
    lateinit var polyService: ItemService

    @MockK
    lateinit var router: BlockchainRouter<ItemService>

    @MockK
    lateinit var itemRepository: ItemRepository

    @InjectMockKs
    lateinit var provider: CustomCollectionItemProvider

    @BeforeEach
    fun beforeEach() {
        clearMocks(ethService, polyService, router, itemRepository)
        coEvery { router.getService(BlockchainDto.ETHEREUM) } returns ethService
        coEvery { router.getService(BlockchainDto.POLYGON) } returns polyService
    }

    @Test
    fun `get collection id - ok`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val token = itemId.value.substringBefore(":")

        coEvery { ethService.getItemCollectionId(itemId.value) } returns token

        val result = provider.getItemCollectionId(itemId)

        assertThat(result).isEqualTo(CollectionIdDto(BlockchainDto.ETHEREUM, token))
    }

    @Test
    fun `fetch by item ids - ok`() = runBlocking<Unit> {
        val item1 = randomUnionItem(randomEthItemId())
        val item2 = randomUnionItem(randomPolygonItemId())
        val item3 = randomUnionItem(randomPolygonItemId())

        coEvery { ethService.getItemsByIds(listOf(item1.id.value)) } returns listOf(item1)
        coEvery { polyService.getItemsByIds(listOf(item2.id.value, item3.id.value)) } returns listOf(item2, item3)

        val result = provider.fetch(listOf(item1.id, item2.id, item3.id))

        assertThat(result).isEqualTo(listOf(item1, item2, item3))
    }

    @Test
    fun `fetch items by collection - ok`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        val continuation = randomString()
        val size = 50

        val item = randomUnionItem(randomEthItemId())

        coEvery {
            ethService.getItemsByCollection(collectionId.value, null, continuation, size)
        } returns Page(1, null, listOf(item))

        val result = provider.fetch(collectionId, continuation, size)

        assertThat(result).isEqualTo(listOf(item))
    }

    @Test
    fun `fetch meta - ok`() = runBlocking<Unit> {
        val item1 = randomEthItemId()
        val item2 = randomEthItemId()
        val item3 = randomPolygonItemId()

        val meta1 = randomUnionMeta()
        val meta3 = randomUnionMeta()

        coEvery { ethService.getItemMetaById(item1.value) } returns meta1
        coEvery { ethService.getItemMetaById(item2.value) } throws IllegalArgumentException()
        coEvery { polyService.getItemMetaById(item3.value) } returns meta3

        val result = provider.fetchMeta(listOf(item1, item2, item3))

        assertThat(result).isEqualTo(mapOf(item1 to meta1, item3 to meta3))
    }
}