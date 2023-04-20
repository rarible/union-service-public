package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.model.elastic.EsItem
import com.rarible.protocol.union.core.model.elastic.EsTrait
import com.rarible.protocol.union.core.model.trait.Trait
import com.rarible.protocol.union.core.model.trait.TraitEntry
import com.rarible.protocol.union.core.model.trait.TraitProperty
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.TraitEntryDto
import com.rarible.protocol.union.dto.TraitsDto
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import java.math.BigDecimal

@IntegrationTest
@TestPropertySource(properties = [
    "common.elastic-search.itemsTraitsKeysLimit=3",
    "common.elastic-search.itemsTraitsValuesLimit=15",
])
internal class ItemTraitServiceTest {
    @Autowired
    private lateinit var repository: EsItemRepository

    @Autowired
    private lateinit var itemTraitService: ItemTraitService

    @Autowired
    private lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        elasticsearchTestBootstrapper.bootstrap()
    }


    @TestFactory
    fun `search items traits successfully`(): List<DynamicTest> {
        runBlocking {
            listOf(
                randomEsItem(
                    collection = "1",
                    traits = listOf(
                        EsTrait("key1", "value11"),
                        EsTrait("key3", "value222"),
                        EsTrait("key4", "value22"),
                        EsTrait("key5", "value22"),
                        EsTrait("eyes", "googly")
                    )
                ),
                randomEsItem(
                    collection = "2",
                    traits = listOf(
                        EsTrait("key1", "value22"),
                        EsTrait("key2", "value222"),
                        EsTrait("key6", "value61")
                    )
                ),
                randomEsItem(
                    collection = "3",
                    traits = listOf(
                        EsTrait("key1", "value13"),
                        EsTrait("key2", "value222")
                    )
                ),
                randomEsItem(
                    collection = "4",
                    traits = listOf(
                        EsTrait("key1", "value11")
                    )
                )
            ).map { repository.save(it) }
        }

        return listOf("eyes", "ey", "googly", "goo").map {

            DynamicTest.dynamicTest("search items traits successfully: $it") {
                runBlocking {

                    val collectionIds = listOf("1", "2", "4")

                    val result = itemTraitService.searchTraits(it, collectionIds)

                    assertThat(result.traits .size).isEqualTo(1)
                    assertThat(result.traits[0].key.value).isEqualTo("eyes")
                    assertThat(result.traits[0].values[0].value).isEqualTo("googly")
                }
            }
        }
    }

    @Test
    fun `get collection traits with rarity`(): Unit = runBlocking {
        val collectionId = "1"
        listOf(
            randomEsItem(
                collection = collectionId,
                traits = listOf(
                    EsTrait("eyes", "green"),
                    EsTrait("eyes", "green"),
                    EsTrait("eyes", "green"),
                    EsTrait("background", "yellow"),
                )
            ),
            randomEsItem(
                collection = collectionId,
                traits = listOf(
                    EsTrait("eyes", "blue"),
                    EsTrait("background", "white"),
                )
            ),
            randomEsItem(
                collection = collectionId,
                traits = listOf(
                    EsTrait("eyes", "blue"),
                    EsTrait("eyes", "gray"),
                    EsTrait("background", "black"),
                )
            ),
            randomEsItem(
                collection = collectionId,
                traits = listOf(
                    EsTrait("eyes", "red-gray")
                )
            ),
            randomEsItem(
                collection = "2",
                traits = listOf(
                    EsTrait("eyes", "black"),
                    EsTrait("background", "purple"),
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

        val result: List<Trait> = itemTraitService.getTraitsDistinct(collectionId, properties)

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
    fun `get empty Traits` (): Unit = runBlocking {

        val result = itemTraitService.getTraitsWithRarity(
            randomString(), setOf(
                TraitProperty("eyes", "green")
            )
        )
        assertThat(result.size).isEqualTo(1)

        assertThat(result.first { it.key == "eyes" }
            .rarity).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `get items traits successfully`(): Unit = runBlocking {
        listOf(
            randomEsItem(
                collection = "1",
                traits = listOf(
                    EsTrait("key1", "value11"),
                    EsTrait("key3", "value222"),
                    EsTrait("key4", "value22"),
                    EsTrait("key5", "value22")
                )
            ),
            randomEsItem(
                collection = "2",
                traits = listOf(
                    EsTrait("key1", "value22"),
                    EsTrait("key2", "value222"),
                    EsTrait("key6", "value61")
                )
            ),
            randomEsItem(
                collection = "3",
                traits = listOf(
                    EsTrait("key1", "value13"),
                    EsTrait("key2", "value222")
                )
            ),
            randomEsItem(
                collection = "4",
                traits = listOf(
                    EsTrait("key1", "value11")
                )
            )
        ).map { repository.save(it) }

        val collectionIds = listOf("1", "2", "4")
         val keysLimit = 3
         val valuesLimit = 15

        var result: TraitsDto = itemTraitService.queryTraits(collectionIds, emptyList() )

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

    fun randomEsItem(collection: String? = randomAddress().toString(), traits: List<EsTrait>) = EsItem(
        id = randomString(),
        itemId = randomAddress().toString(),
        blockchain = BlockchainDto.values().random(),
        collection = collection,
        name = randomString(),
        description = randomString(),
        traits = traits,
        creators = listOf(randomAddress().toString()),
        mintedAt = nowMillis(),
        lastUpdatedAt = nowMillis()
    )

    private fun validateTraitEntry(traitEntry: TraitEntryDto, value: String, count: Long) {
        assertThat(traitEntry.value).isEqualTo(value)
        assertThat(traitEntry.count).isEqualTo(count)
    }
}