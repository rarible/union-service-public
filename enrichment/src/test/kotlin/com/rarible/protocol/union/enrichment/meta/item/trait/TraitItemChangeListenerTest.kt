package com.rarible.protocol.union.enrichment.meta.item.trait

import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.model.ItemAttributeCountChange
import com.rarible.protocol.union.enrichment.model.ItemAttributeShort
import com.rarible.protocol.union.enrichment.model.ItemChangeEvent
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortOrder
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.service.TraitService
import com.rarible.protocol.union.enrichment.test.data.randomEnrichmentCollection
import com.rarible.protocol.union.enrichment.test.data.randomItemMetaDownloadEntry
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomShortSellOrder
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.enrichment.util.toItemAttributeShort
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.atomic.AtomicInteger

@ExtendWith(MockKExtension::class)
class TraitItemChangeListenerTest {

    @InjectMockKs
    private lateinit var traitItemChangeListener: TraitItemChangeListener

    @MockK
    private lateinit var traitService: TraitService

    @MockK
    private lateinit var collectionRepository: CollectionRepository

    @BeforeEach
    fun before() = runBlocking<Unit> {
        coEvery {
            collectionRepository.get(any())
        } returns randomEnrichmentCollection().copy(hasTraits = true)
    }

    @ParameterizedTest
    @MethodSource("traitItemsCountChangesCases")
    fun traitItemsCountChanges(case: TestCase) = runBlocking<Unit> {
        with(case) {
            coEvery {
                traitService.changeItemsCount(any(), any())
            } returns Unit

            traitItemChangeListener.onItemChange(ItemChangeEvent(oldItem, newItem))

            val oldCollection = oldItem?.let { CollectionIdDto(it.blockchain, it.metaEntry?.data?.collectionId!!) }
            val newCollection = newItem.let { CollectionIdDto(it.blockchain, it.metaEntry?.data?.collectionId!!) }
            val interaction = AtomicInteger(0)
            if (expDecOld) {
                coVerify {
                    traitService.changeItemsCount(EnrichmentCollectionId(oldCollection!!), OLD_ATTRIBUTES_SHORT.map {
                        ItemAttributeCountChange(
                            attribute = it,
                            totalChange = -1,
                            listedChange = 0,
                        )
                    }.toSet())
                }
                interaction.incrementAndGet()
            }
            if (expIncNew) {
                coVerify {
                    traitService.changeItemsCount(EnrichmentCollectionId(newCollection), NEW_ATTRIBUTES_SHORT.map {
                        ItemAttributeCountChange(
                            attribute = it,
                            totalChange = 1,
                            listedChange = 0,
                        )
                    }.toSet())
                }
                interaction.incrementAndGet()
            }
            coVerify(exactly = interaction.get()) {
                traitService.changeItemsCount(any(), any())
            }
        }
    }

    @Test
    fun `traitItemsCountChanges for changed attributes for the same collection`() = runBlocking<Unit> {
        val collection = randomEthCollectionId()
        coEvery {
            traitService.changeItemsCount(any(), any())
        } returns Unit

        traitItemChangeListener.onItemChange(
            ItemChangeEvent(
                current = item(collection = collection, attributes = OLD_ATTRIBUTES),
                updated = item(collection = collection, attributes = NEW_ATTRIBUTES),
            )
        )
        coVerify {
            traitService.changeItemsCount(
                EnrichmentCollectionId(collection),
                setOf(
                    ItemAttributeCountChange(
                        attribute = ItemAttributeShort(
                            "hat",
                            "big"
                        ),
                        totalChange = 1,
                        listedChange = 0
                    ),
                    ItemAttributeCountChange(
                        attribute = ItemAttributeShort(
                            "hands",
                            "long"
                        ),
                        totalChange = -1,
                        listedChange = 0,
                    )
                )
            )
        }
    }

    @Test
    fun `traitItemsCountChanges for collection without hasTrait mark`() = runBlocking<Unit> {
        val collection = randomEthCollectionId()

        coEvery {
            collectionRepository.get(EnrichmentCollectionId(collection))
        } returns randomEnrichmentCollection(collection).copy(hasTraits = false)

        traitItemChangeListener.onItemChange(
            ItemChangeEvent(
                current = null,
                updated = item(collection = collection, attributes = NEW_ATTRIBUTES)
            )
        )
        coVerify(exactly = 0) {
            traitService.changeItemsCount(any(), any())
        }
    }

    @Test
    fun `item listed attributes not changed`() = runBlocking<Unit> {
        val collection = randomEthCollectionId()
        val oldItem = item(collection = collection, attributes = OLD_ATTRIBUTES)
        val newItem = item(collection = collection, attributes = OLD_ATTRIBUTES, sellOrder = randomShortSellOrder())

        coEvery {
            collectionRepository.get(EnrichmentCollectionId(collection))
        } returns randomEnrichmentCollection(collection).copy(hasTraits = true)
        coEvery {
            traitService.changeItemsCount(any(), any())
        } returns Unit

        traitItemChangeListener.onItemChange(
            ItemChangeEvent(
                current = oldItem,
                updated = newItem,
            )
        )
        coVerify {
            traitService.changeItemsCount(
                EnrichmentCollectionId(collection),
                setOf(
                    ItemAttributeCountChange(
                        attribute = ItemAttributeShort(
                            "hands",
                            "gold"
                        ),
                        totalChange = 0,
                        listedChange = 1
                    ),
                    ItemAttributeCountChange(
                        attribute = ItemAttributeShort(
                            "hands",
                            "long"
                        ),
                        totalChange = 0,
                        listedChange = 1,
                    )
                ),
            )
        }
    }

    @Test
    fun `item unlisted attributes not changed`() = runBlocking<Unit> {
        val collection = randomEthCollectionId()
        val oldItem = item(collection = collection, attributes = OLD_ATTRIBUTES, sellOrder = randomShortSellOrder())
        val newItem = item(collection = collection, attributes = OLD_ATTRIBUTES, sellOrder = null)

        coEvery {
            collectionRepository.get(EnrichmentCollectionId(collection))
        } returns randomEnrichmentCollection(collection).copy(hasTraits = true)
        coEvery {
            traitService.changeItemsCount(any(), any())
        } returns Unit

        traitItemChangeListener.onItemChange(
            ItemChangeEvent(
                current = oldItem,
                updated = newItem,
            )
        )
        coEvery {
            traitService.changeItemsCount(
                EnrichmentCollectionId(collection),
                setOf(
                    ItemAttributeCountChange(
                        attribute = ItemAttributeShort(
                            "hands",
                            "gold"
                        ),
                        totalChange = 0,
                        listedChange = -1
                    ),
                    ItemAttributeCountChange(
                        attribute = ItemAttributeShort(
                            "hands",
                            "long"
                        ),
                        totalChange = 0,
                        listedChange = -1,
                    )
                ),
            )
        }
    }

    @Test
    fun `listed item deleted`() = runBlocking<Unit> {
        val collection = randomEthCollectionId()
        val oldItem = item(collection = collection, attributes = OLD_ATTRIBUTES, sellOrder = randomShortSellOrder())
        val newItem = item(collection = collection, attributes = NEW_ATTRIBUTES, deleted = true)

        coEvery {
            collectionRepository.get(EnrichmentCollectionId(collection))
        } returns randomEnrichmentCollection(collection).copy(hasTraits = true)
        coEvery {
            traitService.changeItemsCount(any(), any())
        } returns Unit

        traitItemChangeListener.onItemChange(
            ItemChangeEvent(
                current = oldItem,
                updated = newItem,
            )
        )
        coVerify {
            traitService.changeItemsCount(
                EnrichmentCollectionId(collection),
                setOf(
                    ItemAttributeCountChange(
                        attribute = ItemAttributeShort(
                            "hands",
                            "gold"
                        ),
                        totalChange = -1,
                        listedChange = -1
                    ),
                    ItemAttributeCountChange(
                        attribute = ItemAttributeShort(
                            "hands",
                            "long"
                        ),
                        totalChange = -1,
                        listedChange = -1,
                    )
                ),
            )
        }
    }

    @Test
    fun `listed item changed attributes`() = runBlocking<Unit> {
        val collection = randomEthCollectionId()
        val oldItem = item(collection = collection, attributes = OLD_ATTRIBUTES, sellOrder = randomShortSellOrder())
        val newItem = item(collection = collection, attributes = NEW_ATTRIBUTES, sellOrder = randomShortSellOrder())
        coEvery {
            collectionRepository.get(EnrichmentCollectionId(collection))
        } returns randomEnrichmentCollection(collection).copy(hasTraits = true)
        coEvery {
            traitService.changeItemsCount(any(), any())
        } returns Unit

        traitItemChangeListener.onItemChange(
            ItemChangeEvent(
                current = oldItem,
                updated = newItem,
            )
        )
        coVerify {
            traitService.changeItemsCount(
                EnrichmentCollectionId(collection),
                setOf(
                    ItemAttributeCountChange(
                        attribute = ItemAttributeShort(
                            "hat",
                            "big"
                        ),
                        totalChange = 1,
                        listedChange = 1,
                    ),
                    ItemAttributeCountChange(
                        attribute = ItemAttributeShort(
                            "hands",
                            "long"
                        ),
                        totalChange = -1,
                        listedChange = -1,
                    )
                ),
            )
        }
    }

    @Test
    fun `listed item changed collection`() = runBlocking<Unit> {
        val collection1 = randomEthCollectionId()
        val collection2 = randomEthCollectionId()

        val oldItem = item(collection = collection1, attributes = OLD_ATTRIBUTES, sellOrder = randomShortSellOrder())
        val newItem = item(collection = collection2, attributes = NEW_ATTRIBUTES, sellOrder = randomShortSellOrder())

        coEvery {
            collectionRepository.get(EnrichmentCollectionId(collection2))
        } returns randomEnrichmentCollection(collection2).copy(hasTraits = true)
        coEvery {
            traitService.changeItemsCount(any(), any())
        } returns Unit

        traitItemChangeListener.onItemChange(
            ItemChangeEvent(
                current = oldItem,
                updated = newItem,
            )
        )

        coVerify {
            traitService.changeItemsCount(
                EnrichmentCollectionId(collection1),
                setOf(
                    ItemAttributeCountChange(
                        attribute = ItemAttributeShort(
                            "hands",
                            "gold"
                        ),
                        totalChange = -1,
                        listedChange = -1
                    ),
                    ItemAttributeCountChange(
                        attribute = ItemAttributeShort(
                            "hands",
                            "long"
                        ),
                        totalChange = -1,
                        listedChange = -1,
                    )
                ),
            )
        }
        coVerify {
            traitService.changeItemsCount(
                EnrichmentCollectionId(collection2),
                setOf(
                    ItemAttributeCountChange(
                        attribute = ItemAttributeShort(
                            "hands",
                            "gold"
                        ),
                        totalChange = 1,
                        listedChange = 1,
                    ),
                    ItemAttributeCountChange(
                        attribute = ItemAttributeShort(
                            "hat",
                            "big"
                        ),
                        totalChange = 1,
                        listedChange = 1,
                    )
                ),
            )
        }
    }

    companion object {
        private val ADDRESS_ONE = randomEthCollectionId()
        private val ADDRESS_TWO = randomEthCollectionId()

        @Suppress("unused")
        @JvmStatic
        fun traitItemsCountChangesCases() = listOf(
            TestCase(
                oldItem = null,
                newItem = item(collection = ADDRESS_ONE),
                expDecOld = false,
                expIncNew = false,
            ),
            TestCase(
                oldItem = null,
                newItem = item(collection = ADDRESS_ONE, attributes = NEW_ATTRIBUTES),
                expDecOld = false,
                expIncNew = true,
            ),
            TestCase(
                oldItem = item(collection = ADDRESS_ONE, attributes = OLD_ATTRIBUTES),
                newItem = item(collection = ADDRESS_ONE, deleted = true, attributes = NEW_ATTRIBUTES),
                expDecOld = true,
                expIncNew = false,
            ),
            TestCase(
                oldItem = item(collection = ADDRESS_ONE, attributes = OLD_ATTRIBUTES),
                newItem = item(collection = ADDRESS_ONE, attributes = OLD_ATTRIBUTES),
                expDecOld = false,
                expIncNew = false,
            ),
            TestCase(
                oldItem = item(collection = ADDRESS_ONE, attributes = OLD_ATTRIBUTES),
                newItem = item(collection = ADDRESS_TWO, attributes = NEW_ATTRIBUTES),
                expDecOld = true,
                expIncNew = true,
            ),
        )

        private fun item(
            collection: CollectionIdDto,
            attributes: List<UnionMetaAttribute> = listOf(),
            sellOrder: ShortOrder? = null,
            deleted: Boolean = false,
        ): ShortItem {
            return randomShortItem().copy(
                blockchain = collection.blockchain,
                bestSellOrder = sellOrder,
                metaEntry = randomItemMetaDownloadEntry(
                    data = randomUnionMeta().copy(
                        collectionId = collection.value,
                        attributes = attributes
                    )
                ),
                deleted = deleted
            )
        }

        private val OLD_ATTRIBUTES = listOf(
            UnionMetaAttribute(
                key = "hands",
                value = "gold"
            ),
            UnionMetaAttribute(
                key = "hands",
                value = "long"
            ),
        )
        private val OLD_ATTRIBUTES_SHORT = OLD_ATTRIBUTES.toSetOfItemAttributeShort()
        private val NEW_ATTRIBUTES = listOf(
            UnionMetaAttribute(
                key = "hands",
                value = "gold"
            ),
            UnionMetaAttribute(
                key = "hat",
                value = "big"
            ),
        )
        private val NEW_ATTRIBUTES_SHORT = NEW_ATTRIBUTES.toSetOfItemAttributeShort()
        private fun List<UnionMetaAttribute>.toSetOfItemAttributeShort() =
            this.mapNotNull { it.toItemAttributeShort() }.toSet()
    }

    data class TestCase(
        val oldItem: ShortItem?,
        val newItem: ShortItem,
        val expDecOld: Boolean,
        val expIncNew: Boolean,
    ) {
        init {
            require(!expDecOld || oldItem != null)
        }
    }
}
