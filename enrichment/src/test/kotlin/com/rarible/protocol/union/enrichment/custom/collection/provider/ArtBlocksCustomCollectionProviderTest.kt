package com.rarible.protocol.union.enrichment.custom.collection.provider

import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.custom.collection.CustomCollectionItemProvider
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class ArtBlocksCustomCollectionProviderTest {

    @MockK
    lateinit var artificialCollectionService: ArtificialCollectionService

    @MockK
    lateinit var customCollectionItemProvider: CustomCollectionItemProvider

    @InjectMockKs
    lateinit var provider: ArtBlocksCustomCollectionProvider

    private val token = "0x259de0ebbd1e3a81ad288d29fac8beae3abbb599"
    private val collectionId = CollectionIdDto(BlockchainDto.ETHEREUM, token)

    @BeforeEach
    fun beforeEach() {
        clearMocks(artificialCollectionService, customCollectionItemProvider)
        coEvery { artificialCollectionService.createArtificialCollection(any(), any(), any(), any()) } returns Unit
    }

    @Test
    fun `get sub-collection - ok, zero project id`() = runBlocking<Unit> {
        val itemId = createItemId(120324)
        val result = provider.getCustomCollection(itemId, null)

        assertThat(result).isEqualTo(collectionId)
    }

    @Test
    fun `get sub-collection - ok, already exists`() = runBlocking<Unit> {
        val itemId = createItemId(9111111)
        val subCollection = CollectionIdDto(BlockchainDto.ETHEREUM, "0x15d8aadd6c6c56f09de8c88ef42c2cf0c52d71b9")

        coEvery { artificialCollectionService.exists(subCollection) } returns true

        val result = provider.getCustomCollection(itemId, null)

        assertThat(result).isEqualTo(subCollection)
        coVerify(exactly = 0) { artificialCollectionService.createArtificialCollection(any(), any(), any(), any()) }
    }

    @Test
    fun `get sub-collection - ok, created`() = runBlocking<Unit> {
        val itemId = createItemId(123111111)
        val subCollection = CollectionIdDto(BlockchainDto.ETHEREUM, "0x814ec5e9a5e91837887cd7b78c39fa9038c7a239")
        val meta = randomUnionMeta(attributes = listOf(UnionMetaAttribute("collection_name", "123")))

        coEvery { artificialCollectionService.exists(subCollection) } returns false
        coEvery { customCollectionItemProvider.getMeta(listOf(itemId)) } returns mapOf(itemId to meta)

        val result = provider.getCustomCollection(itemId, null)

        assertThat(result).isEqualTo(subCollection)
        coVerify(exactly = 1) {
            artificialCollectionService.createArtificialCollection(
                collectionId,
                subCollection,
                "123",
                UnionCollection.Structure.PART
            )
        }
    }

    @Test
    fun `get sub-collection - ok, created, meta attribute not found`() = runBlocking<Unit> {
        val itemId = createItemId(987987111111)
        val subCollection = CollectionIdDto(BlockchainDto.ETHEREUM, "0x4755745f20b8b7944f85abfffb62549dc8f6d4dd")
        val meta = randomUnionMeta()

        coEvery { artificialCollectionService.exists(subCollection) } returns false
        coEvery { customCollectionItemProvider.getMeta(listOf(itemId)) } returns mapOf(itemId to meta)

        val result = provider.getCustomCollection(itemId, null)

        assertThat(result).isEqualTo(subCollection)
        coVerify(exactly = 1) {
            artificialCollectionService.createArtificialCollection(
                collectionId,
                subCollection,
                null,
                UnionCollection.Structure.PART
            )
        }
    }

    private fun createItemId(tokenId: Long): ItemIdDto {
        return ItemIdDto(BlockchainDto.ETHEREUM, "$token:$tokenId")
    }
}