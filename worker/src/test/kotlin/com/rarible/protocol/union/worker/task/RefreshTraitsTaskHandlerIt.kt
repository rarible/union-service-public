package com.rarible.protocol.union.worker.task

import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.enrichment.model.Trait
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.test.data.randomEnrichmentCollection
import com.rarible.protocol.union.enrichment.test.data.randomItemMetaDownloadEntry
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomShortSellOrder
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.worker.IntegrationTest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoOperations

@IntegrationTest
class RefreshTraitsTaskHandlerIt {
    @Autowired
    lateinit var collectionRepository: CollectionRepository

    @Autowired
    lateinit var itemRepository: ItemRepository

    @Autowired
    lateinit var mongo: ReactiveMongoOperations

    @Autowired
    lateinit var refreshTraitsTaskHandler: RefreshTraitsTaskHandler

    @Test
    fun `refresh traits`() = runBlocking<Unit> {
        val collection = randomEnrichmentCollection()
        collectionRepository.save(collection)
        itemRepository.save(
            randomShortItem()
                .copy(
                    collectionId = collection.id.toString(),
                    metaEntry = randomItemMetaDownloadEntry()
                        .copy(
                            data = randomUnionMeta(
                                attributes = listOf(
                                    UnionMetaAttribute("key1", "value1"),
                                    UnionMetaAttribute("key2")
                                )
                            )
                        )
                )
        )
        itemRepository.save(
            randomShortItem()
                .copy(
                    collectionId = collection.id.toString(),
                    metaEntry = randomItemMetaDownloadEntry()
                        .copy(
                            data = randomUnionMeta(
                                attributes = listOf(
                                    UnionMetaAttribute("key1", "value1"),
                                    UnionMetaAttribute("key2"),
                                    UnionMetaAttribute("key3", "value3")
                                )
                            )
                        )
                )
        )
        itemRepository.save(
            randomShortItem()
                .copy(
                    collectionId = collection.id.toString(),
                    bestSellOrder = randomShortSellOrder(),
                    metaEntry = randomItemMetaDownloadEntry()
                        .copy(
                            data = randomUnionMeta(
                                attributes = listOf(
                                    UnionMetaAttribute("key1", "value1"),
                                    UnionMetaAttribute("key2"),
                                    UnionMetaAttribute("key3", "value3"),
                                    UnionMetaAttribute("key4", "value4")
                                )
                            )
                        )
                )
        )

        refreshTraitsTaskHandler.runLongTask(null, "").toList()

        val afterRefresh = mongo.findAll(Trait::class.java).asFlow().toList()

        assertThat(afterRefresh).containsExactlyInAnyOrder(
            Trait(
                collectionId = collection.id,
                key = "key1",
                value = "value1",
                itemsCount = 3,
                listedItemsCount = 1,
            ),
            Trait(
                collectionId = collection.id,
                key = "key2",
                value = null,
                itemsCount = 3,
                listedItemsCount = 1,
            ),
            Trait(
                collectionId = collection.id,
                key = "key3",
                value = "value3",
                itemsCount = 2,
                listedItemsCount = 1,
            ),
            Trait(
                collectionId = collection.id,
                key = "key4",
                value = "value4",
                itemsCount = 1,
                listedItemsCount = 1,
            )
        )
    }
}
