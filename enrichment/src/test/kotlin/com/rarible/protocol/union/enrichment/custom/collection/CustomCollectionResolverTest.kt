package com.rarible.protocol.union.enrichment.custom.collection

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.configuration.CustomCollectionMapping
import com.rarible.protocol.union.enrichment.configuration.CustomCollectionMetaAttributeMapping
import com.rarible.protocol.union.enrichment.configuration.CustomCollectionMetaMapping
import com.rarible.protocol.union.enrichment.configuration.EnrichmentCollectionProperties
import com.rarible.protocol.union.enrichment.custom.collection.mapper.CollectionMapperIndex
import com.rarible.protocol.union.enrichment.custom.collection.provider.CustomCollectionProviderFactory
import com.rarible.protocol.union.enrichment.test.data.randomItemMetaDownloadEntry
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
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
        every { getService(BlockchainDto.POLYGON) } returns itemService
    }

    private val customCollectionProviderFactory = CustomCollectionProviderFactory(mockk())
    private val customCollectionItemProvider: CustomCollectionItemProvider = mockk()

    private lateinit var resolver: CustomCollectionResolver

    @Test
    fun `by item - ok, via item list`() = runBlocking<Unit> {
        val customCollectionId = randomEthCollectionId()
        val itemId = randomEthItemId()

        resolver = createResolver(customCollectionId, items = listOf(itemId))
        val collection = resolveByItem(randomString(), itemId)

        assertThat(collection).isEqualTo(customCollectionId)
    }

    @Test
    fun `by item - not mapped, via item list`() = runBlocking<Unit> {
        val customCollectionId = randomEthCollectionId()
        val itemId = randomEthItemId()

        resolver = createResolver(customCollectionId, items = listOf(itemId))
        val collection1 = resolveByItem("a", randomEthItemId())
        val collection2 = resolveByItem(randomString(), itemId.copy(blockchain = BlockchainDto.POLYGON))

        assertThat(collection1).isNull()
        assertThat(collection2).isNull()
    }

    @Test
    fun `by item - ok, via collection`() = runBlocking<Unit> {
        val customCollectionId = randomEthCollectionId()
        val collectionId = randomEthCollectionId()
        val itemId = ItemIdDto(collectionId.blockchain, "${collectionId.value}:1")

        resolver = createResolver(customCollectionId, collections = listOf(collectionId))
        val collection = resolveByItem(randomString(), itemId)

        assertThat(collection).isEqualTo(customCollectionId)
    }

    @Test
    fun `by item - not mapped, via collection`() = runBlocking<Unit> {
        val customCollectionId = randomEthCollectionId()
        val collectionId = randomEthCollectionId()
        val itemId = ItemIdDto(BlockchainDto.POLYGON, "${collectionId.value}:1")

        resolver = createResolver(customCollectionId, items = listOf(itemId))
        val collection1 = resolveByItem(randomString(), randomEthItemId())
        val collection2 = resolveByItem(randomString(), randomEthItemId())

        assertThat(collection1).isNull()
        assertThat(collection2).isNull()
    }

    @Test
    fun `by collection - ok`() = runBlocking<Unit> {
        val customCollectionId = randomEthCollectionId()
        val collectionId = randomEthCollectionId()

        resolver = createResolver(customCollectionId, collections = listOf(collectionId))
        val collection = resolveByCollection(randomString(), collectionId)

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

        resolver = createResolver(
            customCollectionId,
            ranges = listOf("${collectionId.fullId()}:1..5", "${collectionId.fullId()}:10..10")
        )

        assertThat(resolveByItem(randomString(), itemId1)).isEqualTo(customCollectionId)
        assertThat(resolveByItem(randomString(), itemId2)).isEqualTo(customCollectionId)
        assertThat(resolveByItem(randomString(), itemId3)).isEqualTo(customCollectionId)
        assertThat(resolveByItem(randomString(), itemId4)).isEqualTo(customCollectionId)
    }

    @Test
    fun `by range - not mapped`() = runBlocking<Unit> {
        val customCollectionId = randomEthCollectionId()
        val collectionId = randomEthCollectionId()

        val itemId1 = ItemIdDto(collectionId.blockchain, "${collectionId.value}:5")
        val itemId2 = ItemIdDto(collectionId.blockchain, "${collectionId.value}:8")

        resolver = createResolver(
            customCollectionId,
            ranges = listOf("${collectionId.fullId()}:6..7")
        )

        assertThat(resolveByItem(randomString(), itemId1)).isNull()
        assertThat(resolveByItem(randomString(), itemId2)).isNull()
    }

    @Test
    fun `by collection - not mapped`() = runBlocking<Unit> {
        val customCollectionId = randomEthCollectionId()
        val collectionId = randomEthCollectionId()

        resolver = createResolver(customCollectionId, collections = listOf(collectionId))
        val collection = resolveByCollection(randomString(), randomEthCollectionId())

        assertThat(collection).isNull()
    }

    @Test
    fun `by meta - ok`() = runBlocking<Unit> {
        val customCollectionId = randomEthCollectionId()
        val collectionId = randomEthCollectionId()
        val itemId = ItemIdDto(collectionId.blockchain, "${collectionId.value}:1")

        val item = randomShortItem().copy(
            metaEntry = randomItemMetaDownloadEntry(
                data = randomUnionMeta().copy(attributes = listOf(UnionMetaAttribute("a", "b")))
            )
        )
        coEvery { customCollectionItemProvider.getOrFetchMeta(listOf(itemId)) } returns mapOf(itemId to item)

        val customMetaMapping = CustomCollectionMetaMapping(
            listOf(collectionId.fullId()),
            listOf(CustomCollectionMetaAttributeMapping("a", setOf("b")))
        )

        resolver = createResolver(customCollectionId, meta = customMetaMapping)

        assertThat(resolveByItem(itemId, itemId)).isEqualTo(customCollectionId)
    }

    @Test
    fun `by meta - not mapped`() = runBlocking<Unit> {
        val customCollectionId = randomEthCollectionId()
        val collectionId = randomEthCollectionId()
        val itemId = ItemIdDto(collectionId.blockchain, "${collectionId.value}:1")

        val item = randomShortItem().copy(
            metaEntry = randomItemMetaDownloadEntry(
                data = randomUnionMeta().copy(attributes = listOf(UnionMetaAttribute("key1", "b")))
            )
        )

        coEvery { customCollectionItemProvider.getOrFetchMeta(listOf(itemId)) } returns mapOf(itemId to item)

        val customMetaMapping = CustomCollectionMetaMapping(
            listOf(collectionId.fullId()),
            listOf(
                CustomCollectionMetaAttributeMapping("key1", setOf("c")),
                CustomCollectionMetaAttributeMapping("key2", setOf("b"))
            )
        )

        resolver = createResolver(customCollectionId, meta = customMetaMapping)

        assertThat(resolveByItem(itemId, itemId)).isNull()
    }

    private fun createResolver(
        customCollection: CollectionIdDto,
        items: List<ItemIdDto> = emptyList(),
        collections: List<CollectionIdDto> = emptyList(),
        ranges: List<String> = emptyList(),
        meta: CustomCollectionMetaMapping = CustomCollectionMetaMapping()
    ): CustomCollectionResolver {
        val mapping = CustomCollectionMapping(
            name = customCollection.fullId(),
            items = items.map { it.fullId() },
            collections = collections.map { it.fullId() },
            ranges = ranges,
            meta = meta
        )
        val properties = EnrichmentCollectionProperties(listOf(mapping))
        val index = CollectionMapperIndex(
            customCollectionItemProvider,
            customCollectionProviderFactory,
            FeatureFlagsProperties(),
            properties
        )

        return CustomCollectionResolver(
            router,
            index
        )
    }

    private suspend fun <T> resolveByItem(key: T, itemId: ItemIdDto): CollectionIdDto? {
        val request = listOf(CustomCollectionResolutionRequest(key, itemId, null))
        return resolver.resolve(request)
            .get(key)
    }

    private suspend fun <T> resolveByCollection(key: T, collectionId: CollectionIdDto): CollectionIdDto? {
        val request = listOf(CustomCollectionResolutionRequest(key, null, collectionId))
        return resolver.resolve(request)
            .get(key)
    }
}
