package com.rarible.protocol.union.integration.immutablex.repository

import com.rarible.protocol.union.integration.immutablex.model.ImxCollectionMetaSchema
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues

class ImxCollectionMetaSchemaRepository(
    private val mongo: ReactiveMongoTemplate,
) {

    suspend fun getById(id: String): ImxCollectionMetaSchema? {
        return mongo.findById<ImxCollectionMetaSchema>(id).awaitFirstOrNull()
    }

    suspend fun getAll(ids: Collection<String>): List<ImxCollectionMetaSchema> {
        val criteria = Criteria("_id").inValues(ids)
        return mongo.find<ImxCollectionMetaSchema>(Query(criteria)).collectList().awaitFirst()
    }

    suspend fun save(collectionSchema: ImxCollectionMetaSchema) {
        mongo.save(collectionSchema).awaitFirst()
    }
}
