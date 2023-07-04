package com.rarible.protocol.union.enrichment.service

import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component

@Component
class EnrichmentBlacklistService(
    private val template: ReactiveMongoTemplate
) {

    private val collection = "enrichment_blacklist"

    suspend fun isBlacklisted(id: String): Boolean {
        val query = Query(Criteria("_id").isEqualTo(id))
        return template.exists(query, collection)
            .awaitSingle()
    }
}