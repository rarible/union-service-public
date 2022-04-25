package com.rarible.protocol.union.enrichment.repository.search

import com.rarible.protocol.union.core.model.EsCollection
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.stereotype.Component

@Component
class EsCollectionRepository(
    private val esOperations: ReactiveElasticsearchOperations,
) {

    private val clazz = EsCollection::class.java

    suspend fun saveAll(collections: List<EsCollection>): List<EsCollection> {
        return esOperations.saveAll(collections, clazz).collectList().awaitSingle()
    }

    suspend fun findById(collectionId: String): EsCollection? {
        return esOperations.get(collectionId, clazz).awaitFirstOrNull()
    }
}
