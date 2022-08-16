package com.rarible.protocol.union.integration.immutablex.cache

import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues

class ImxCollectionCreatorRepository(
    private val mongo: ReactiveMongoTemplate,
) {

    suspend fun getAll(ids: Collection<String>): List<ImxCollectionCreator> {
        val criteria = Criteria("_id").inValues(ids)
        return mongo.find<ImxCollectionCreator>(Query(criteria)).collectList().awaitFirst()
    }

    suspend fun saveAll(creators: List<ImxCollectionCreator>) {
        creators.forEach { mongo.save(it).awaitFirst() }
    }

}