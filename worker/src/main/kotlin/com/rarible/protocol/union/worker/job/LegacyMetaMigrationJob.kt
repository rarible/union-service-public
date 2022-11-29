package com.rarible.protocol.union.worker.job

import com.rarible.core.common.nowMillis
import com.rarible.core.loader.internal.common.LoadTask
import com.rarible.core.loader.internal.common.MongoLoadTaskRepository
import com.rarible.protocol.union.enrichment.meta.item.migration.ItemMetaMigrator
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.asFlow
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component

@Component
@Deprecated("Should be removed after migration")
class LegacyMetaMigrationJob(
    private val mongo: ReactiveMongoOperations,
    private val itemMetaMigrator: ItemMetaMigrator
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun migrate(fromTask: String?, chunkSize: Int) = flow {
        val criteria = fromTask?.let { Criteria("_id").gt(ObjectId(it)) } ?: Criteria()
        val query = Query(criteria)
        query.with(Sort.by(Sort.Direction.ASC, "_id"))
        mongo.find<LoadTask>(query, MongoLoadTaskRepository.COLLECTION)
            .buffer(chunkSize)
            .asFlow()
            .collect { tasks ->
                val now = nowMillis()
                itemMetaMigrator.migrate(tasks, chunkSize)
                logger.info(
                    "Migrated ${tasks.size} meta tasks, took ${nowMillis().toEpochMilli() - now.toEpochMilli()}ms"
                )
                tasks.lastOrNull()?.let { emit(it.id) }
            }
    }
}
