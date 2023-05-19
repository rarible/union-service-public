package com.rarible.protocol.union.enrichment.custom.collection

import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.configuration.CustomCollectionMapping
import com.rarible.protocol.union.enrichment.configuration.EnrichmentCollectionProperties
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CustomCollectionResolverTest {

    private val itemService: ItemService = mockk() {
        coEvery { getItemCollectionId(any()) } answers { it.invocation.args[0].toString().substringBefore(":") }
    }

    private val router: BlockchainRouter<ItemService> = mockk() {
        every { getService(BlockchainDto.ETHEREUM) } returns itemService
    }

    @Test
    fun `by item - ok, via item list`() = runBlocking<Unit> {
        val customCollectionId = randomEthCollectionId()
        val itemId = randomEthItemId()

        val resolver = createResolver(customCollectionId, items = listOf(itemId))
        val collection = resolver.resolveCustomCollection(itemId)

        assertThat(collection).isEqualTo(customCollectionId)
    }

    @Test
    fun `by item - not mapped, via item list`() = runBlocking<Unit> {
        val customCollectionId = randomEthCollectionId()
        val itemId = randomEthItemId()

        val resolver = createResolver(customCollectionId, items = listOf(itemId))
        val collection1 = resolver.resolveCustomCollection(randomEthItemId())
        val collection2 = resolver.resolveCustomCollection(itemId.copy(blockchain = BlockchainDto.POLYGON))

        assertThat(collection1).isNull()
        assertThat(collection2).isNull()
    }

    @Test
    fun `by item - ok, via collection`() = runBlocking<Unit> {
        val customCollectionId = randomEthCollectionId()
        val collectionId = randomEthCollectionId()
        val itemId = ItemIdDto(collectionId.blockchain, "${collectionId.value}:1")

        val resolver = createResolver(customCollectionId, collections = listOf(collectionId))
        val collection = resolver.resolveCustomCollection(itemId)

        assertThat(collection).isEqualTo(customCollectionId)
    }

    @Test
    fun `by item - not mapped, via collection`() = runBlocking<Unit> {
        val customCollectionId = randomEthCollectionId()
        val collectionId = randomEthCollectionId()
        val itemId = ItemIdDto(BlockchainDto.POLYGON, "${collectionId.value}:1")

        val resolver = createResolver(customCollectionId, items = listOf(itemId))
        val collection1 = resolver.resolveCustomCollection(randomEthItemId())
        val collection2 = resolver.resolveCustomCollection(randomEthItemId())

        assertThat(collection1).isNull()
        assertThat(collection2).isNull()
    }

    @Test
    fun `by collection - ok`() = runBlocking<Unit> {
        val customCollectionId = randomEthCollectionId()
        val collectionId = randomEthCollectionId()

        val resolver = createResolver(customCollectionId, collections = listOf(collectionId))
        val collection = resolver.resolveCustomCollection(collectionId)

        assertThat(collection).isEqualTo(customCollectionId)
    }

    @Test
    fun `by range - ok`() = runBlocking<Unit> {
        val customCollectionId = randomEthCollectionId()
        val collectionId = randomEthCollectionId()

        val itemId1 = ItemIdDto(collectionId.blockchain, "${collectionId.value}:1")
        val itemId2 = ItemIdDto(collectionId.blockchain, "${collectionId.value}:3")
        val itemId3 = ItemIdDto(collectionId.blockchain, "${collectionId.value}:5")
        val itemId4 = ItemIdDto(collectionId.blockchain, "${collectionId.value}:10")

        val resolver = createResolver(
            customCollectionId,
            ranges = listOf("${collectionId.fullId()}:1..5", "${collectionId.fullId()}:10..10")
        )

        assertThat(resolver.resolveCustomCollection(itemId1)).isEqualTo(customCollectionId)
        assertThat(resolver.resolveCustomCollection(itemId2)).isEqualTo(customCollectionId)
        assertThat(resolver.resolveCustomCollection(itemId3)).isEqualTo(customCollectionId)
        assertThat(resolver.resolveCustomCollection(itemId4)).isEqualTo(customCollectionId)
    }

    @Test
    fun `by range - not mapped`() = runBlocking<Unit> {
        val customCollectionId = randomEthCollectionId()
        val collectionId = randomEthCollectionId()

        val itemId1 = ItemIdDto(collectionId.blockchain, "${collectionId.value}:5")
        val itemId2 = ItemIdDto(collectionId.blockchain, "${collectionId.value}:8")

        val resolver = createResolver(
            customCollectionId,
            ranges = listOf("${collectionId.fullId()}:6..7")
        )

        assertThat(resolver.resolveCustomCollection(itemId1)).isNull()
        assertThat(resolver.resolveCustomCollection(itemId2)).isNull()
    }

    @Test
    fun `by collection - not mapped`() = runBlocking<Unit> {
        val customCollectionId = randomEthCollectionId()
        val collectionId = randomEthCollectionId()

        val resolver = createResolver(customCollectionId, collections = listOf(collectionId))
        val collection = resolver.resolveCustomCollection(randomEthCollectionId())

        assertThat(collection).isNull()
    }

    private fun createResolver(
        customCollection: CollectionIdDto,
        items: List<ItemIdDto> = emptyList(),
        collections: List<CollectionIdDto> = emptyList(),
        ranges: List<String> = emptyList()
    ): CustomCollectionResolver {
        val mapping = CustomCollectionMapping(
            customCollection = customCollection.fullId(),
            items = items.map { it.fullId() },
            collections = collections.map { it.fullId() },
            ranges = ranges
        )
        val properties = EnrichmentCollectionProperties(listOf(mapping))

        return CustomCollectionResolver(router, properties, FeatureFlagsProperties(enableCustomCollections = true))
    }

}
