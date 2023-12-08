package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.OrdersPaginationDto
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.repository.ItemMetaRepository
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.repository.TraitRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentItemEventService
import com.rarible.protocol.union.enrichment.test.data.randomEnrichmentCollection
import com.rarible.protocol.union.enrichment.test.data.randomItemMetaDownloadEntry
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrder
import com.rarible.protocol.union.enrichment.util.TraitUtils
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthSellOrderDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.every
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono

@IntegrationTest
class InternalItemChangeEventHandlerFt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var traitRepository: TraitRepository

    @Autowired
    private lateinit var enrichmentItemEventService: EnrichmentItemEventService

    @Autowired
    private lateinit var itemMetaRepository: ItemMetaRepository

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var collectionRepository: CollectionRepository

    @Test
    fun `handle - item change event, meta and order`() = runBlocking {
        val collectionId = randomEthCollectionId()
        val collection = randomEnrichmentCollection(collectionId).copy(hasTraits = true)
        collectionRepository.save(collection)

        val attribute1 = UnionMetaAttribute(randomString(), randomString())
        val attribute2 = UnionMetaAttribute(randomString(), randomString())
        val traitId1: String = TraitUtils.getId(collection.id, attribute1.key, attribute1.value!!)
        val traitId2: String = TraitUtils.getId(collection.id, attribute2.key, attribute2.value!!)

        val itemId = randomEthItemId()
        val item = randomShortItem(itemId).copy(
            collectionId = collectionId.value,
            bestSellOrder = null,
            blockchain = collectionId.blockchain,
            metaEntry = randomItemMetaDownloadEntry(data = null)
        )
        itemRepository.save(item)

        itemMetaRepository.update(
            entryId = itemId.fullId(),
            isUpdateRequired = { true },
            updateEntry = {
                randomItemMetaDownloadEntry(
                    data = randomUnionMeta().copy(
                        attributes = listOf(
                            attribute1,
                            attribute2
                        )
                    )
                )
            }
        )

        waitAssert {
            val trait1 = traitRepository.get(traitId1)
            assertThat(trait1?.itemsCount).isEqualTo(1)
            assertThat(trait1?.listedItemsCount).isEqualTo(0)

            val trait2 = traitRepository.get(traitId2)
            assertThat(trait2?.itemsCount).isEqualTo(1)
            assertThat(trait2?.listedItemsCount).isEqualTo(0)
        }

        every {
            testEthereumItemApi.getNftItemById(any())
        } returns randomEthNftItemDto().toMono()
        every {
            testEthereumOrderApi.getByIds(any())
        } returns OrdersPaginationDto(
            orders = listOf(randomEthSellOrderDto(itemId)),
        ).toMono()

        enrichmentItemEventService.onItemBestSellOrderUpdated(
            itemId = item.id,
            order = randomUnionSellOrder(itemId),
            eventTimeMarks = null,
        )
        waitAssert {
            val trait1 = traitRepository.get(traitId1)
            assertThat(trait1?.itemsCount).isEqualTo(1)
            assertThat(trait1?.listedItemsCount).isEqualTo(1)

            val trait2 = traitRepository.get(traitId2)
            assertThat(trait2?.itemsCount).isEqualTo(1)
            assertThat(trait2?.listedItemsCount).isEqualTo(1)
        }

        enrichmentItemEventService.onItemBestSellOrderUpdated(
            itemId = item.id,
            order = randomUnionSellOrder(itemId),
            eventTimeMarks = null,
        )
        waitAssert {
            val trait1 = traitRepository.get(traitId1)
            assertThat(trait1?.itemsCount).isEqualTo(1)
            assertThat(trait1?.listedItemsCount).isEqualTo(1)

            val trait2 = traitRepository.get(traitId2)
            assertThat(trait2?.itemsCount).isEqualTo(1)
            assertThat(trait2?.listedItemsCount).isEqualTo(1)
        }
    }

    @Test
    fun `handle - item change event, delete`() = runBlocking {
        val collectionId = randomEthCollectionId()
        val collection = randomEnrichmentCollection(collectionId).copy(hasTraits = true)
        collectionRepository.save(collection)

        val attribute1 = UnionMetaAttribute(randomString(), randomString())
        val attribute2 = UnionMetaAttribute(randomString(), randomString())
        val traitId1: String = TraitUtils.getId(collection.id, attribute1.key, attribute1.value!!)
        val traitId2: String = TraitUtils.getId(collection.id, attribute2.key, attribute2.value!!)

        val itemId = randomEthItemId()
        val item = randomShortItem(itemId).copy(
            bestSellOrder = null,
            blockchain = collectionId.blockchain,
            metaEntry = randomItemMetaDownloadEntry(data = null)
        )
        itemRepository.save(item)

        itemMetaRepository.update(
            entryId = itemId.fullId(),
            isUpdateRequired = { true },
            updateEntry = {
                randomItemMetaDownloadEntry(
                    data = randomUnionMeta().copy(
                        collectionId = collectionId.value,
                        attributes = listOf(
                            attribute1,
                            attribute2
                        )
                    )
                )
            }
        )

        waitAssert {
            val savedItem = itemRepository.get(item.id)
            assertThat(savedItem?.deleted).isFalse()

            val trait1 = traitRepository.get(traitId1)
            assertThat(trait1?.itemsCount).isEqualTo(1)
            assertThat(trait1?.listedItemsCount).isEqualTo(0)

            val trait2 = traitRepository.get(traitId2)
            assertThat(trait2?.itemsCount).isEqualTo(1)
            assertThat(trait2?.listedItemsCount).isEqualTo(0)
        }

        every {
            testEthereumItemApi.getNftItemById(any())
        } returns randomEthNftItemDto().toMono()
        every {
            testEthereumOrderApi.getByIds(any())
        } returns OrdersPaginationDto(
            orders = listOf(randomEthSellOrderDto(itemId)),
        ).toMono()

        enrichmentItemEventService.onItemDeleted(
            UnionItemDeleteEvent(
                itemId = item.id.toDto(),
                null
            )
        )
        waitAssert {
            val savedItem = itemRepository.get(item.id)
            assertThat(savedItem?.deleted).isTrue()

            val trait1 = traitRepository.get(traitId1)
            assertThat(trait1?.itemsCount).isEqualTo(0)
            assertThat(trait1?.listedItemsCount).isEqualTo(0)

            val trait2 = traitRepository.get(traitId2)
            assertThat(trait2?.itemsCount).isEqualTo(0)
            assertThat(trait2?.listedItemsCount).isEqualTo(0)
        }
    }
}
