package com.rarible.protocol.union.integration.immutablex.repository

import com.rarible.protocol.union.integration.immutablex.model.ImxItemMeta
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues

class ImxItemMetaRepository(
    private val mongo: ReactiveMongoTemplate,
) {

    suspend fun getAll(ids: Collection<String>): List<ImxItemMeta> {
        val criteria = Criteria("_id").inValues(ids)
        return mongo.find<ImxItemMeta>(Query(criteria)).collectList().awaitFirst()
    }

    suspend fun save(itemMeta: ImxItemMeta) {
        mongo.save(itemMeta).awaitFirst()
    }
}
