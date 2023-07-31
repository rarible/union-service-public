package com.rarible.protocol.union.enrichment.migration

import com.mongodb.reactivestreams.client.MongoClients
import com.rarible.core.test.containers.MongodbTestContainer
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.enrichment.migration.ChangeLog00003DeleteCollectionStatistics.Companion.STATISTICS
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

class DeleteCollectionStatistics {

    private val mongoTest = MongodbTestContainer()
    private val mongoTemplate = ReactiveMongoTemplate(MongoClients.create(mongoTest.connectionString()), "test")

    @Test
    fun `should remove statistics from collections`() = runBlocking<Unit> {
        mongoTemplate.insert(
            mapOf(
                "collectionId" to randomString(),
                STATISTICS to "b"
            ),
            "enrichment_collection"
        ).awaitFirstOrNull()

        val migration = ChangeLog00003DeleteCollectionStatistics()
        migration.deleteCollectionStatistics(mongoTemplate)

        val collection =
            mongoTemplate.find(Query(Criteria.where(STATISTICS).exists(true)), EnrichmentCollection::class.java)
                .awaitFirstOrNull()
        assertThat(collection).isNull()
    }
}
