package com.rarible.protocol.union.enrichment.repository.search

import com.rarible.protocol.union.core.model.EsCollection
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.stereotype.Component

@Component
class EsCollectionRepository(
    private val esOperations: ReactiveElasticsearchOperations,
) {

    suspend fun saveAll(collections: List<EsCollection>): List<EsCollection> {
        return esOperations.saveAll(collections, EsCollection::class.java).collectList().awaitSingle()
    }
}
