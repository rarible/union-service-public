package com.rarible.protocol.union.enrichment.repository

import com.rarible.protocol.union.enrichment.model.Trait
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.index.PartialIndexFilter
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component

@Component
class TraitRepository(
    private val template: ReactiveMongoTemplate
) {
    val collection: String = template.getCollectionName(Trait::class.java)

    suspend fun createIndices() {
        ALL_INDEXES.forEach { index ->
            logger.info("Ensure index '{}' for collection '{}'", index, collection)
            template.indexOps(collection).ensureIndex(index).awaitSingle()
        }
    }

    suspend fun save(item: Trait): Trait {
        return template.save(item).awaitFirst()
    }

    suspend fun get(id: String): Trait? {
        return template.findById<Trait>(id).awaitFirstOrNull()
    }

    suspend fun getAll(ids: List<String>): List<Trait> {
        if (ids.isEmpty()) return emptyList()
        val criteria = Criteria("_id").inValues(ids)
        return template.find<Trait>(Query(criteria)).collectList().awaitFirst()
    }

    suspend fun deleteWithZeroItemsCount() =
        template.findAllAndRemove(
            Query(Criteria(Trait::itemsCount.name).lte(0L)),
            Trait::class.java
        ).asFlow()

    private val logger = LoggerFactory.getLogger(ItemRepository::class.java)

    companion object {
        private val COLLECTION_KEY_DEFINITION = Index()
            .on(Trait::collectionId.name, Sort.Direction.ASC)
            .on(Trait::key.name, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .background()

        private val COLLECTION_ITEMS_COUNT_DEFINITION = Index()
            .on(Trait::itemsCount.name, Sort.Direction.ASC)
            .partial(PartialIndexFilter.of(Trait::itemsCount isEqualTo 0L))
            .background()

        private val ALL_INDEXES = listOf(
            COLLECTION_KEY_DEFINITION,
            COLLECTION_ITEMS_COUNT_DEFINITION,
        )
    }
}
