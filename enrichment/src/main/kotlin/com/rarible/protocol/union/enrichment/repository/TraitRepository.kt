package com.rarible.protocol.union.enrichment.repository

import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.enrichment.model.ItemAttributeShort
import com.rarible.protocol.union.enrichment.model.Trait
import com.rarible.protocol.union.enrichment.util.TraitUtils
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
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

    suspend fun incrementItemsCount(
        collectionId: CollectionIdDto,
        attribute: ItemAttributeShort,
        incTotal: Long = 1,
        incListed: Long = 1
    ): String {
        val (key, value) = attribute
        val traitId = TraitUtils.getId(
            collectionId = collectionId,
            key = key,
            value = value,
        )
        template.upsert(
            Query(where(Trait::id).isEqualTo(traitId)),
            Update().inc(Trait::itemsCount.name, incTotal)
                .inc(Trait::listedItemsCount.name, incListed)
                .inc(Trait::version.name, 1)
                .setOnInsert(Trait::key.name, key)
                .setOnInsert(Trait::value.name, value)
                .setOnInsert(Trait::collectionId.name, collectionId)
                .setOnInsert("_class", Trait::class.java.name),
            Trait::class.java,
        ).awaitSingle()
        return traitId
    }

    private val logger = LoggerFactory.getLogger(ItemRepository::class.java)

    companion object {
        private val COLLECTION_KEY_DEFINITION = Index()
            .on(Trait::collectionId.name, Sort.Direction.ASC)
            .on(Trait::key.name, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .background()

        private val ALL_INDEXES = listOf(
            COLLECTION_KEY_DEFINITION,
        )
    }
}
