package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.api.configuration.EsProperties
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.api.service.api.CollectionApiService
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.model.elastic.EsItem
import com.rarible.protocol.union.core.model.elastic.EsItemTrait
import com.rarible.protocol.union.core.model.trait.Trait
import com.rarible.protocol.union.core.model.trait.TraitEntry
import com.rarible.protocol.union.core.model.trait.TraitProperty
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.TraitEntryDto
import com.rarible.protocol.union.dto.TraitsDto
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.test.data.randomEnrichmentCollection
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import java.math.BigDecimal

@IntegrationTest
internal class ItemTraitServiceTest {
    @Autowired
    private lateinit var repository: EsItemRepository

    @Autowired
    private lateinit var elasticClient: ReactiveElasticsearchClient

    @Autowired
    private lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    @Autowired
    private lateinit var collectionApiService: CollectionApiService

    @Autowired
    private lateinit var collectionRepository: CollectionRepository

    private lateinit var itemTraitService: ItemTraitService

    private val properties = EsProperties(
        itemsTraitsKeysLimit = 3,
        itemsTraitsValuesLimit = 15
    )

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        itemTraitService = ItemTraitService(
            elasticClient = elasticClient,
            esItemRepository = repository,
            esProperties = properties,
            collectionApiService = collectionApiService,
            collectionRepository = collectionRepository,
        )
        elasticsearchTestBootstrapper.bootstrap()
    }

    @TestFactory
    fun `search items traits successfully`(): List<DynamicTest> = runBlocking {
        val collection1 = collectionRepository.save(randomEnrichmentCollection().copy(hasTraits = true))
        val collection2 = collectionRepository.save(randomEnrichmentCollection().copy(hasTraits = true))
        val collection3 = collectionRepository.save(randomEnrichmentCollection().copy(hasTraits = true))
        val collection4 = collectionRepository.save(randomEnrichmentCollection().copy(hasTraits = true))
        val collection5 = collectionRepository.save(randomEnrichmentCollection().copy(hasTraits = false))
        listOf(
            randomEsItem(
                collection = collection1.id.toString(),
                traits = listOf(
                    EsItemTrait("key1", "value11"),
                    EsItemTrait("key3", "value222"),
                    EsItemTrait("key4", "value22"),
                    EsItemTrait("key5", "value22"),
                    EsItemTrait("eyes", "googly")
                )
            ),
            randomEsItem(
                collection = collection2.id.toString(),
                traits = listOf(
                    EsItemTrait("key1", "value22"),
                    EsItemTrait("key2", "value222"),
                    EsItemTrait("key6", "value61")
                )
            ),
            randomEsItem(
                collection = collection3.id.toString(),
                traits = listOf(
                    EsItemTrait("key1", "value13"),
                    EsItemTrait("key2", "value222")
                )
            ),
            randomEsItem(
                collection = collection4.id.toString(),
                traits = listOf(
                    EsItemTrait("key1", "value11")
                )
            ),
            randomEsItem(
                collection = collection5.id.toString(),
                traits = listOf(
                    EsItemTrait("key1", "value11"),
                    EsItemTrait("key3", "value222"),
                    EsItemTrait("key4", "value22"),
                    EsItemTrait("key5", "value22"),
                    EsItemTrait("key6", "value61"),
                    EsItemTrait("eyes", "googly")
                )
            ),
        ).map { repository.save(it) }

        listOf("eyes", "ey", "googly", "goo").map {

            DynamicTest.dynamicTest("search items traits successfully: $it") {
                runBlocking {

                    val collectionIds = listOf(
                        collection1.id.toString(),
                        collection2.id.toString(),
                        collection4.id.toString(),
                        collection5.id.toString()
                    )

                    val result = itemTraitService.searchTraits(it, collectionIds)

                    assertThat(result.traits.size).isEqualTo(1)
                    assertThat(result.traits[0].key.value).isEqualTo("eyes")
                    assertThat(result.traits[0].values[0].value).isEqualTo("googly")
                }
            }
        }
    }

    @Test
    fun `get collection traits with rarity`(): Unit = runBlocking {
        val collection1 = collectionRepository.save(randomEnrichmentCollection().copy(hasTraits = true))
        val collection2 = collectionRepository.save(randomEnrichmentCollection().copy(hasTraits = true))
        listOf(
            randomEsItem(
                collection = collection1.id.toString(),
                traits = listOf(
                    EsItemTrait("eyes", "green"),
                    EsItemTrait("eyes", "green"),
                    EsItemTrait("eyes", "green"),
                    EsItemTrait("background", "yellow"),
                )
            ),
            randomEsItem(
                collection = collection1.id.toString(),
                traits = listOf(
                    EsItemTrait("eyes", "blue"),
                    EsItemTrait("background", "white"),
                )
            ),
            randomEsItem(
                collection = collection1.id.toString(),
                traits = listOf(
                    EsItemTrait("eyes", "blue"),
                    EsItemTrait("eyes", "gray"),
                    EsItemTrait("background", "black"),
                )
            ),
            randomEsItem(
                collection = collection1.id.toString(),
                traits = listOf(
                    EsItemTrait("eyes", "red-gray")
                )
            ),
            randomEsItem(
                collection = collection2.id.toString(),
                traits = listOf(
                    EsItemTrait("eyes", "black"),
                    EsItemTrait("background", "purple"),
                )
            )
        ).map { repository.save(it) }

        val properties = setOf(
            TraitProperty("eyes", "green"),
            TraitProperty("eyes", "blue"),
            TraitProperty("eyes", "gray"),
            TraitProperty("eyes", "black"),
            TraitProperty("eyes", "red-gray"),
            TraitProperty("background", "yellow"),
            TraitProperty("background", "white"),
            TraitProperty("background", "black"),
            TraitProperty("background", "purple")
        )

        val result: List<Trait> = itemTraitService.getTraitsDistinct(collection1.id.toString(), properties)

        assertThat(result.size).isEqualTo(2)
        val keys = result.map { it.key }
        assertThat(keys).containsExactlyInAnyOrder(TraitEntry("eyes", 4), TraitEntry("background", 3))

        val eyesTraits = result.first { it.key.value == "eyes" }.values
        assertThat(eyesTraits).containsExactlyInAnyOrder(
            TraitEntry("green", 1),
            TraitEntry("blue", 2),
            TraitEntry("gray", 1),
            TraitEntry("red-gray", 1),
            TraitEntry("black", 0)
        )

        val backgroundTraits = result.first { it.key.value == "background" }.values
        assertThat(backgroundTraits).containsExactlyInAnyOrder(
            TraitEntry("yellow", 1),
            TraitEntry("white", 1),
            TraitEntry("black", 1),
            TraitEntry("purple", 0),
        )
    }

    @Test
    fun `get empty Traits`(): Unit = runBlocking {
        val collection1 = collectionRepository.save(randomEnrichmentCollection().copy(hasTraits = true))
        val result = itemTraitService.getTraitsWithRarity(
            collection1.id.toString(),
            setOf(
                TraitProperty("eyes", "green")
            )
        )
        assertThat(result.size).isEqualTo(1)

        assertThat(result.first { it.key == "eyes" }
            .rarity).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `get items traits successfully`(): Unit = runBlocking {
        val collection1 = collectionRepository.save(randomEnrichmentCollection().copy(hasTraits = true))
        val collection2 = collectionRepository.save(randomEnrichmentCollection().copy(hasTraits = true))
        val collection3 = collectionRepository.save(randomEnrichmentCollection().copy(hasTraits = true))
        val collection4 = collectionRepository.save(randomEnrichmentCollection().copy(hasTraits = true))
        val collection5 = collectionRepository.save(randomEnrichmentCollection().copy(hasTraits = false))
        listOf(
            randomEsItem(
                collection = collection1.id.toString(),
                traits = listOf(
                    EsItemTrait("key1", "value11"),
                    EsItemTrait("key3", "value222"),
                    EsItemTrait("key4", "value22"),
                    EsItemTrait("key5", "value22")
                )
            ),
            randomEsItem(
                collection = collection2.id.toString(),
                traits = listOf(
                    EsItemTrait("key1", "value22"),
                    EsItemTrait("key2", "value222"),
                    EsItemTrait("key6", "value61")
                )
            ),
            randomEsItem(
                collection = collection3.id.toString(),
                traits = listOf(
                    EsItemTrait("key1", "value13"),
                    EsItemTrait("key2", "value222")
                )
            ),
            randomEsItem(
                collection = collection4.id.toString(),
                traits = listOf(
                    EsItemTrait("key1", "value11")
                )
            ),
            randomEsItem(
                collection = collection5.id.toString(),
                traits = listOf(
                    EsItemTrait("key1", "value11"),
                    EsItemTrait("key3", "value222"),
                    EsItemTrait("key4", "value22"),
                    EsItemTrait("key5", "value22")
                )
            ),
        ).map { repository.save(it) }

        val collectionIds = listOf(
            collection1.id.toString(),
            collection2.id.toString(),
            collection4.id.toString(),
            collection5.id.toString()
        )
        val keysLimit = 3
        val valuesLimit = 15

        var result: TraitsDto = itemTraitService.queryTraits(collectionIds, emptyList())

        assertThat(result.traits.size).isEqualTo(keysLimit)

        validateTraitEntry(traitEntry = result.traits[0].key, value = "key1", count = 3)
        var values = result.traits[0].values
        assertThat(values.size).isEqualTo(2)
        validateTraitEntry(traitEntry = values[0], value = "value11", count = 2)
        validateTraitEntry(traitEntry = values[1], value = "value22", count = 1)

        validateTraitEntry(traitEntry = result.traits[1].key, value = "key2", count = 1)
        values = result.traits[1].values
        assertThat(values.size).isEqualTo(1)
        validateTraitEntry(traitEntry = values[0], value = "value222", count = 1)

        validateTraitEntry(traitEntry = result.traits[2].key, value = "key3", count = 1)
        values = result.traits[2].values
        assertThat(values.size).isEqualTo(1)
        validateTraitEntry(traitEntry = values[0], value = "value222", count = 1)

        result = itemTraitService.queryTraits(collectionIds, listOf("key1"))
        assertThat(result.traits.size).isEqualTo(1)
        validateTraitEntry(traitEntry = result.traits[0].key, value = "key1", count = 3)

        result = itemTraitService.queryTraits(collectionIds, listOf("key1", "key6"))
        assertThat(result.traits.size).isEqualTo(2)
        validateTraitEntry(traitEntry = result.traits[0].key, value = "key1", count = 3)
        validateTraitEntry(traitEntry = result.traits[1].key, value = "key6", count = 1)

        result = itemTraitService.queryTraits(collectionIds, listOf("key2"))
        assertThat(result.traits.size).isEqualTo(1)
        validateTraitEntry(traitEntry = result.traits[0].key, value = "key2", count = 1)
        validateTraitEntry(traitEntry = result.traits[0].values[0], value = "value222", count = 1)
    }

    fun randomEsItem(collection: String? = randomAddress().toString(), traits: List<EsItemTrait>): EsItem {
        val blockchain = BlockchainDto.ETHEREUM
        val itemId = ItemIdDto(blockchain, randomAddress().prefixed(), randomBigInt())
        return EsItem(
            id = randomString(),
            itemId = itemId.fullId(),
            blockchain = blockchain,
            collection = collection,
            token = itemId.value.split(":")[0],
            tokenId = itemId.value.split(":")[1],
            name = randomString(),
            description = randomString(),
            traits = traits,
            creators = listOf(randomAddress().toString()),
            mintedAt = nowMillis(),
            lastUpdatedAt = nowMillis()
        )
    }

    private fun validateTraitEntry(traitEntry: TraitEntryDto, value: String, count: Long) {
        assertThat(traitEntry.value).isEqualTo(value)
        assertThat(traitEntry.count).isEqualTo(count)
    }
}
