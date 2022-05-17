package com.rarible.protocol.union.core.elasticsearch.repository

import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.model.EsCollection
import com.rarible.protocol.union.core.model.EsOrder
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.stereotype.Component

@Component
class EsCollectionRepository(
    private val esOperations: ReactiveElasticsearchOperations,
    esNameResolver: EsNameResolver
) {
    val entityDefinition = esNameResolver.createEntityDefinitionExtended(EsOrder.ENTITY_DEFINITION)
    private val clazz = EsCollection::class.java

    suspend fun saveAll(collections: List<EsCollection>): List<EsCollection> {
        return esOperations.saveAll(collections, entityDefinition.writeIndexCoordinates).collectList().awaitSingle()
    }

    suspend fun findById(collectionId: String): EsCollection? {
        return esOperations.get(collectionId, clazz, entityDefinition.searchIndexCoordinates).awaitFirstOrNull()
    }
}
