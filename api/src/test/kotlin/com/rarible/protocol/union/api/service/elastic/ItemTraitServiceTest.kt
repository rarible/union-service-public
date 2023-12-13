package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.api.configuration.EsProperties
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.elastic.EsTraitFilter
import com.rarible.protocol.union.core.model.trait.Trait
import com.rarible.protocol.union.core.model.trait.TraitEntry
import com.rarible.protocol.union.core.model.trait.TraitProperty
import com.rarible.protocol.union.dto.ExtendedTraitPropertyDto
import com.rarible.protocol.union.dto.TraitDto
import com.rarible.protocol.union.dto.TraitEntryDto
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.repository.search.EsTraitRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal

@ExtendWith(MockKExtension::class)
class ItemTraitServiceTest {

    @MockK
    private lateinit var legacyItemTraitService: LegacyItemTraitService

    @MockK
    private lateinit var esTraitRepository: EsTraitRepository

    @MockK
    private lateinit var esItemRepository: EsItemRepository

    @MockK
    private lateinit var ff: FeatureFlagsProperties

    private val esProperties = mockk<EsProperties> {
        every { itemsTraitsKeysLimit } returns 100
        every { itemsTraitsValuesLimit } returns 100
    }

    @InjectMockKs
    private lateinit var itemTraitService: ItemTraitService

    @Test
    fun `getTraitsWithRarity - optimised` () = runBlocking<Unit> {
        val collectionId = randomString()
        val properties1 = TraitProperty(key = randomString(), randomString())
        val properties2 = TraitProperty(key = randomString(), randomString())
        val properties = setOf(properties1, properties2)

        every { ff.enableOptimizedSearchForTraits } returns true
        coEvery { esItemRepository.countItemsInCollection(collectionId) } returns 100
        coEvery { esTraitRepository.getTraits(collectionId, properties) } returns listOf(
            Trait(
                key = TraitEntry(
                    value = "key1",
                    count = 10,
                ),
                values = listOf(
                    TraitEntry(
                        value = "value1",
                        count = 20,
                    ),
                    TraitEntry(
                        value = "value2",
                        count = 30,
                    ),
                )
            ),
            Trait(
                key = TraitEntry(
                    value = "key2",
                    count = 15,
                ),
                values = listOf(
                    TraitEntry(
                        value = "value3",
                        count = 40,
                    ),
                    TraitEntry(
                        value = "value4",
                        count = 50,
                    ),
                )
            ),
        )

        val result = itemTraitService.getTraitsWithRarity(
            collectionId = collectionId,
            properties = properties
        )
        assertThat(result).hasSize(4)
        assertThat(result).containsExactlyElementsOf(
            listOf(
                ExtendedTraitPropertyDto(
                    key = "key1",
                    value = "value1",
                    rarity = BigDecimal("20.0000000"),
                ),
                ExtendedTraitPropertyDto(
                    key = "key1",
                    value = "value2",
                    rarity = BigDecimal("30.0000000"),
                ),
                ExtendedTraitPropertyDto(
                    key = "key2",
                    value = "value3",
                    rarity = BigDecimal("40.0000000"),
                ),
                ExtendedTraitPropertyDto(
                    key = "key2",
                    value = "value4",
                    rarity = BigDecimal("50.0000000"),
                ),
            )
        )
    }

    @Test
    fun `searchTraits - optimised` () = runBlocking<Unit> {
        val collectionId1 = randomString()
        val collectionId2 = randomString()
        val filter = "test"

        every { ff.enableOptimizedSearchForTraits } returns true

        coEvery {
            esTraitRepository.searchTraits(
                EsTraitFilter(
                    text = filter,
                    collectionIds = setOf(collectionId1, collectionId2),
                    keysLimit = 100,
                    valuesLimit = 100,
                )
            )
        } returns listOf(
            Trait(
                key = TraitEntry(
                    value = "key1",
                    count = 10,
                ),
                values = listOf(
                    TraitEntry(
                        value = "value1",
                        count = 20,
                    ),
                    TraitEntry(
                        value = "value2",
                        count = 30,
                    ),
                )
            ),
            Trait(
                key = TraitEntry(
                    value = "key2",
                    count = 40,
                ),
                values = listOf(
                    TraitEntry(
                        value = "value3",
                        count = 50,
                    ),
                    TraitEntry(
                        value = "value4",
                        count = 60,
                    ),
                )
            ),
        )

        val result = itemTraitService.searchTraits(
            filter = filter,
            collectionIds = listOf(collectionId1, collectionId2),
        )
        assertThat(result.traits).containsExactlyElementsOf(
            listOf(
                TraitDto(
                    key = TraitEntryDto(
                        value = "key1",
                        count = 10
                    ),
                    values = listOf(
                        TraitEntryDto(
                            value = "value1",
                            count = 20
                        ),
                        TraitEntryDto(
                            value = "value2",
                            count = 30
                        ),
                    )
                ),
                TraitDto(
                    key = TraitEntryDto(
                        value = "key2",
                        count = 40
                    ),
                    values = listOf(
                        TraitEntryDto(
                            value = "value3",
                            count = 50
                        ),
                        TraitEntryDto(
                            value = "value4",
                            count = 60
                        ),
                    )
                ),
            )
        )
    }
}
