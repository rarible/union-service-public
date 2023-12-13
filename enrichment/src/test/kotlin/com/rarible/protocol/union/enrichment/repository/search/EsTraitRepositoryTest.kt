package com.rarible.protocol.union.enrichment.repository.search

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.protocol.union.core.model.elastic.EsSortOrder
import com.rarible.protocol.union.core.model.elastic.EsTrait
import com.rarible.protocol.union.core.model.elastic.EsTraitFilter
import com.rarible.protocol.union.core.model.trait.Trait
import com.rarible.protocol.union.core.model.trait.TraitEntry
import com.rarible.protocol.union.core.model.trait.TraitProperty
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.test.IntegrationTest
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

@IntegrationTest
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class EsTraitRepositoryTest {
    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var esTraitRepository: EsTraitRepository

    @Test
    fun crud() = runBlocking<Unit> {
        val trait = EsTrait(
            id = UUID.randomUUID().toString(),
            blockchain = BlockchainDto.ETHEREUM,
            collection = randomEthCollectionId().fullId(),
            key = "key",
            value = "value",
            itemsCount = 0,
            listedItemsCount = 0,
            version = 1
        )
        index(trait)

        val saved = esTraitRepository.findById(trait.id)
        assertThat(saved).isEqualTo(trait)
        val update1 = trait.copy(itemsCount = 2, listedItemsCount = 4, version = 2)

        index(update1)

        val updated = esTraitRepository.findById(trait.id)
        assertThat(updated).isEqualTo(update1)

        esTraitRepository.bulk(idsToDelete = listOf(trait.id))
        refreshIndex()

        assertThat(esTraitRepository.findById(trait.id)).isNull()
    }

    @Test
    fun searchTraits() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId().fullId()
        val collectionId2 = randomEthCollectionId().fullId()
        val collectionId3 = randomEthCollectionId().fullId()

        index(
            EsTrait(
                id = UUID.randomUUID().toString(),
                blockchain = BlockchainDto.ETHEREUM,
                collection = collectionId,
                key = "key1",
                value = "value1",
                itemsCount = 2,
                listedItemsCount = 1,
                version = 1
            )
        )
        index(
            EsTrait(
                id = UUID.randomUUID().toString(),
                blockchain = BlockchainDto.ETHEREUM,
                collection = collectionId,
                key = "key1",
                value = "value2",
                itemsCount = 1,
                listedItemsCount = 0,
                version = 1
            )
        )
        index(
            EsTrait(
                id = UUID.randomUUID().toString(),
                blockchain = BlockchainDto.ETHEREUM,
                collection = collectionId,
                key = "key1",
                value = "value3",
                itemsCount = 3,
                listedItemsCount = 2,
                version = 1
            )
        )
        index(
            EsTrait(
                id = UUID.randomUUID().toString(),
                blockchain = BlockchainDto.ETHEREUM,
                collection = collectionId,
                key = "key1",
                value = "value4",
                itemsCount = 10,
                listedItemsCount = 9,
                version = 1
            )
        )
        index(
            EsTrait(
                id = UUID.randomUUID().toString(),
                blockchain = BlockchainDto.ETHEREUM,
                collection = collectionId,
                key = "key2",
                value = "value1",
                itemsCount = 11,
                listedItemsCount = 10,
                version = 1
            )
        )
        index(
            EsTrait(
                id = UUID.randomUUID().toString(),
                blockchain = BlockchainDto.ETHEREUM,
                collection = collectionId,
                key = "key3",
                value = "value1",
                itemsCount = 12,
                listedItemsCount = 11,
                version = 1
            )
        )
        index(
            EsTrait(
                id = UUID.randomUUID().toString(),
                blockchain = BlockchainDto.ETHEREUM,
                collection = collectionId,
                key = "key4",
                value = "value1",
                itemsCount = 13,
                listedItemsCount = 12,
                version = 1
            )
        )
        index(
            EsTrait(
                id = UUID.randomUUID().toString(),
                blockchain = BlockchainDto.ETHEREUM,
                collection = collectionId2,
                key = "key5",
                value = "value2",
                itemsCount = 14,
                listedItemsCount = 13,
                version = 1
            )
        )
        index(
            EsTrait(
                id = UUID.randomUUID().toString(),
                blockchain = BlockchainDto.ETHEREUM,
                collection = collectionId3,
                key = "key1",
                value = "value1",
                itemsCount = 1,
                listedItemsCount = 0,
                version = 1
            )
        )

        val result = esTraitRepository.searchTraits(
            filter = EsTraitFilter(
                keys = emptySet(),
                listed = false,
                collectionIds = setOf(collectionId, collectionId2),
                valueFrequencySortOrder = EsSortOrder.ASC,
                keysLimit = 100,
                valuesLimit = 100
            )
        )
        assertThat(result).containsExactly(
            Trait(
                key = TraitEntry(value = "key1", count = 6),
                values = listOf(
                    TraitEntry(value = "value2", count = 1),
                    TraitEntry(value = "value1", count = 2),
                    TraitEntry(value = "value3", count = 3),
                )
            ),
            Trait(
                key = TraitEntry(value = "key2", count = 11),
                values = listOf(
                    TraitEntry(value = "value1", count = 11),
                )
            ),
            Trait(
                key = TraitEntry(value = "key3", count = 12),
                values = listOf(
                    TraitEntry(value = "value1", count = 12),
                )
            )
        )

        val resultDesc = esTraitRepository.searchTraits(
            filter = EsTraitFilter(
                keys = emptySet(),
                listed = false,
                collectionIds = setOf(collectionId, collectionId2),
                valueFrequencySortOrder = EsSortOrder.DESC,
                keysLimit = 100,
                valuesLimit = 100
            ),
        )
        assertThat(resultDesc).containsExactly(
            Trait(
                key = TraitEntry(value = "key1", count = 15),
                values = listOf(
                    TraitEntry(value = "value4", count = 10),
                    TraitEntry(value = "value3", count = 3),
                    TraitEntry(value = "value1", count = 2),
                )
            ),
            Trait(
                key = TraitEntry(value = "key2", count = 11),
                values = listOf(
                    TraitEntry(value = "value1", count = 11),
                )
            ),
            Trait(
                key = TraitEntry(value = "key3", count = 12),
                values = listOf(
                    TraitEntry(value = "value1", count = 12),
                )
            )
        )

        val keysFiltered = esTraitRepository.searchTraits(
            filter = EsTraitFilter(
                keys = setOf("key1"),
                listed = false,
                collectionIds = setOf(collectionId, collectionId2),
                valueFrequencySortOrder = EsSortOrder.ASC,
                keysLimit = 100,
                valuesLimit = 100
            )
        )
        assertThat(keysFiltered).containsExactly(
            Trait(
                key = TraitEntry(value = "key1", count = 6),
                values = listOf(
                    TraitEntry(value = "value2", count = 1),
                    TraitEntry(value = "value1", count = 2),
                    TraitEntry(value = "value3", count = 3),
                )
            )
        )

        val collectionFiltered = esTraitRepository.searchTraits(
            filter = EsTraitFilter(
                keys = emptySet(),
                listed = false,
                collectionIds = setOf(collectionId2),
                valueFrequencySortOrder = EsSortOrder.ASC,
                keysLimit = 100,
                valuesLimit = 100
            )
        )
        assertThat(collectionFiltered).containsExactly(
            Trait(
                key = TraitEntry(value = "key5", count = 14), values = listOf(TraitEntry(value = "value2", count = 14))
            )
        )

        val textFiltered = esTraitRepository.searchTraits(
            filter = EsTraitFilter(
                text = "value2",
                listed = false,
                keys = emptySet(),
                collectionIds = emptySet(),
                valueFrequencySortOrder = EsSortOrder.ASC,
                keysLimit = 100,
                valuesLimit = 100
            )
        )

        assertThat(textFiltered).containsExactly(
            Trait(
                key = TraitEntry(value = "key1", count = 1),
                values = listOf(
                    TraitEntry(value = "value2", count = 1),
                )
            ),
            Trait(
                key = TraitEntry(value = "key5", count = 14), values = listOf(TraitEntry(value = "value2", count = 14))
            )
        )

        val listed = esTraitRepository.searchTraits(
            filter = EsTraitFilter(
                keys = emptySet(),
                listed = true,
                collectionIds = setOf(collectionId, collectionId2),
                valueFrequencySortOrder = EsSortOrder.ASC,
                keysLimit = 100,
                valuesLimit = 100
            )
        )
        assertThat(listed).containsExactly(
            Trait(
                key = TraitEntry(value = "key1", count = 12),
                values = listOf(
                    TraitEntry(value = "value1", count = 1),
                    TraitEntry(value = "value3", count = 2),
                    TraitEntry(value = "value4", count = 9),
                )
            ),
            Trait(
                key = TraitEntry(value = "key2", count = 10),
                values = listOf(
                    TraitEntry(value = "value1", count = 10),
                )
            ),
            Trait(
                key = TraitEntry(value = "key3", count = 11),
                values = listOf(
                    TraitEntry(value = "value1", count = 11),
                )
            )
        )
    }

    @Test
    fun getTraits() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId().fullId()
        val collectionId2 = randomEthCollectionId().fullId()

        index(
            EsTrait(
                id = UUID.randomUUID().toString(),
                blockchain = BlockchainDto.ETHEREUM,
                collection = collectionId,
                key = "key1",
                value = "value1",
                itemsCount = 2,
                listedItemsCount = 1,
                version = 1
            )
        )
        index(
            EsTrait(
                id = UUID.randomUUID().toString(),
                blockchain = BlockchainDto.ETHEREUM,
                collection = collectionId,
                key = "key1",
                value = "value2",
                itemsCount = 3,
                listedItemsCount = 2,
                version = 1
            )
        )
        index(
            EsTrait(
                id = UUID.randomUUID().toString(),
                blockchain = BlockchainDto.ETHEREUM,
                collection = collectionId,
                key = "key2",
                value = "value1",
                itemsCount = 4,
                listedItemsCount = 3,
                version = 1
            )
        )
        index(
            EsTrait(
                id = UUID.randomUUID().toString(),
                blockchain = BlockchainDto.ETHEREUM,
                collection = collectionId2,
                key = "key1",
                value = "value1",
                itemsCount = 5,
                listedItemsCount = 4,
                version = 1
            )
        )

        val result = esTraitRepository.getTraits(
            collectionId = "collectionId",
            properties = setOf(
                TraitProperty(key = "key1", value = "value1"),
                TraitProperty(key = "key1", value = "value2"),
                TraitProperty(key = "key666", value = "value123"),
            )
        )

        assertThat(result).hasSize(2)
        assertThat(result[0].key).isEqualTo(TraitEntry(value = "key1", count = 5))
        assertThat(result[0].values).containsExactlyInAnyOrder(
            TraitEntry(value = "value1", count = 2),
            TraitEntry(value = "value2", count = 3),
        )
        assertThat(result[1].key).isEqualTo(TraitEntry(value = "key666", count = 0))
        assertThat(result[1].values).containsExactlyInAnyOrder(
            TraitEntry(value = "value123", count = 0),
        )
    }

    private suspend fun index(trait: EsTrait): EsTrait {
        esTraitRepository.bulk(listOf(trait))
        refreshIndex()
        return trait
    }

    private suspend fun refreshIndex() {
        esTraitRepository.refresh()
    }
}
