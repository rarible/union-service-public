package com.rarible.protocol.union.enrichment.meta.collection.provider

import com.rarible.protocol.union.core.model.download.DownloadException
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.configuration.CustomCollectionMapping
import com.rarible.protocol.union.enrichment.configuration.EnrichmentCollectionProperties
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.service.SimpleHashService
import com.rarible.protocol.union.enrichment.test.data.randomCollectionMetaDownloadEntry
import com.rarible.protocol.union.enrichment.test.data.randomEnrichmentCollection
import com.rarible.protocol.union.enrichment.test.data.randomSimpleHashItem
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class ArtBlocksCollectionMetaCustomProviderTest {

    private val artBlocksCollection = randomEnrichmentCollection().copy(metaEntry = randomCollectionMetaDownloadEntry())

    private val artBlocksMapping = CustomCollectionMapping(
        name = "artblocks",
        collections = listOf(artBlocksCollection.id.toString())
    )

    @SpyK
    var properties = EnrichmentCollectionProperties(mappings = listOf(artBlocksMapping))

    @MockK
    lateinit var simpleHashService: SimpleHashService

    @MockK
    lateinit var collectionRepository: CollectionRepository

    @InjectMockKs
    lateinit var provider: ArtBlocksCollectionMetaCustomProvider

    @BeforeEach
    fun beforeEach() {
        clearMocks(simpleHashService, collectionRepository)
    }

    @Test
    fun `fetch - fail, not exists`() = runBlocking<Unit> {
        val collection = randomEnrichmentCollection()

        coEvery { collectionRepository.get(collection.id) } returns null

        val result = provider.fetch(collection.id.blockchain, collection.id.collectionId)

        assertThat(result.supported).isFalse()
    }

    @Test
    fun `fetch - fail, no parent`() = runBlocking<Unit> {
        val collection = randomEnrichmentCollection().copy(
            parent = null,
            extra = mapOf("project_id" to "1")
        )

        coEvery { collectionRepository.get(collection.id) } returns collection

        val result = provider.fetch(collection.id.blockchain, collection.id.collectionId)
        assertThat(result.supported).isFalse()
    }

    @Test
    fun `fetch - fail, no project id`() = runBlocking<Unit> {
        val collection = randomEnrichmentCollection().copy(
            parent = artBlocksCollection.id,
            extra = emptyMap()
        )

        coEvery { collectionRepository.get(collection.id) } returns collection

        val result = provider.fetch(collection.id.blockchain, collection.id.collectionId)
        assertThat(result.supported).isFalse()
    }

    @Test
    fun `fetch - fail, parent not found`() = runBlocking<Unit> {
        val collection = randomEnrichmentCollection().copy(
            parent = artBlocksCollection.id,
            extra = mapOf("project_id" to "1")
        )

        coEvery { collectionRepository.get(collection.id) } returns collection
        coEvery { collectionRepository.get(artBlocksCollection.id) } returns null

        assertThrows(DownloadException::class.java) {
            runBlocking { provider.fetch(collection.id.blockchain, collection.id.collectionId) }
        }
    }

    @Test
    fun `fetch - fail, parent without meta`() = runBlocking<Unit> {
        val collection = randomEnrichmentCollection().copy(
            parent = artBlocksCollection.id,
            extra = mapOf("project_id" to "1")
        )

        coEvery { collectionRepository.get(collection.id) } returns collection
        coEvery { collectionRepository.get(artBlocksCollection.id) } returns artBlocksCollection.copy(
            metaEntry = null
        )

        assertThrows(DownloadException::class.java) {
            runBlocking { provider.fetch(collection.id.blockchain, collection.id.collectionId) }
        }
    }

    @Test
    fun `fetch - fail, simplehash item not found`() = runBlocking<Unit> {
        val collection = randomEnrichmentCollection().copy(
            parent = artBlocksCollection.id,
            extra = mapOf("project_id" to "1")
        )
        val firstItemId = IdParser.parseItemId("${artBlocksCollection.id}:1000000")

        coEvery { collectionRepository.get(collection.id) } returns collection
        coEvery { collectionRepository.get(artBlocksCollection.id) } returns artBlocksCollection
        coEvery { simpleHashService.fetchRaw(firstItemId) } returns null

        assertThrows(DownloadException::class.java) {
            runBlocking { provider.fetch(collection.id.blockchain, collection.id.collectionId) }
        }
    }

    @Test
    fun `fetch - ok, images taken from collection`() = runBlocking<Unit> {
        val collection = randomEnrichmentCollection().copy(
            parent = artBlocksCollection.id,
            extra = mapOf("project_id" to "1")
        )
        val firstItemId = IdParser.parseItemId("${artBlocksCollection.id}:1000000")
        val item = randomSimpleHashItem()

        coEvery { collectionRepository.get(collection.id) } returns collection
        coEvery { collectionRepository.get(artBlocksCollection.id) } returns artBlocksCollection
        coEvery { simpleHashService.fetchRaw(firstItemId) } returns item

        val result = provider.fetch(collection.id.blockchain, collection.id.collectionId).data!!

        val shCollection = item.collection!!
        assertThat(result.name).isEqualTo(item.extraMetadata?.collectionName)
        assertThat(result.description).isEqualTo(item.description)
        assertThat(result.content[0].url).isEqualTo(shCollection.imageUrl)
        assertThat(result.content[0].representation).isEqualTo(MetaContentDto.Representation.ORIGINAL)
        assertThat(result.content[1].url).isEqualTo(shCollection.bannerImageUrl)
        assertThat(result.content[1].representation).isEqualTo(MetaContentDto.Representation.BIG)
    }

    @Test
    fun `fetch - ok, image taken from item`() = runBlocking<Unit> {
        val collection = randomEnrichmentCollection().copy(
            parent = artBlocksCollection.id,
            extra = mapOf("project_id" to "1")
        )
        val firstItemId = IdParser.parseItemId("${artBlocksCollection.id}:1000000")
        val item = randomSimpleHashItem(collection = null)

        coEvery { collectionRepository.get(collection.id) } returns collection
        coEvery { collectionRepository.get(artBlocksCollection.id) } returns artBlocksCollection
        coEvery { simpleHashService.fetchRaw(firstItemId) } returns item

        val result = provider.fetch(collection.id.blockchain, collection.id.collectionId).data!!

        assertThat(result.name).isEqualTo(item.extraMetadata?.collectionName)
        assertThat(result.description).isEqualTo(item.description)
        assertThat(result.content[0].url).isEqualTo(item.extraMetadata?.imageOriginalUrl)
        assertThat(result.content[0].representation).isEqualTo(MetaContentDto.Representation.ORIGINAL)
    }
}
