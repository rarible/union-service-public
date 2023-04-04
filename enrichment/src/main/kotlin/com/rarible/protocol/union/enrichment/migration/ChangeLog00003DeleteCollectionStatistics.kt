package com.rarible.protocol.union.enrichment.migration

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

@ChangeLog(order = "2")
class ChangeLog00003DeleteCollectionStatistics {

    @ChangeSet(
        id = "ChangeLog00003DeleteCollectionStatistics.deleteCollectionStatistics",
        order = "1",
        author = "protocol"
    )
    fun deleteCollectionStatistics(
        @NonLockGuarded template: ReactiveMongoTemplate
    ) = runBlocking {
        val query = Query(Criteria.where(STATISTICS).exists(true))
        template.updateMulti(query, Update().unset(STATISTICS), EnrichmentCollection::class.java).awaitFirstOrNull()
    }

    companion object {
        const val STATISTICS = "statistics"
    }
}
