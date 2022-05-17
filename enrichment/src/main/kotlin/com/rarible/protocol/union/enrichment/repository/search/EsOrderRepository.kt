package com.rarible.protocol.union.enrichment.repository.search

import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.model.EsOrder
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class EsOrderRepository(
    private val esOperations: ReactiveElasticsearchOperations,
    esNameResolver: EsNameResolver
) {
    val entityDefinition = esNameResolver.createEntityDefinitionExtended(EsOrder.ENTITY_DEFINITION)

    suspend fun findById(id: String): EsOrder? {
        return esOperations.get(id, EsOrder::class.java, entityDefinition.searchIndexCoordinates).awaitFirstOrNull()
    }

    suspend fun save(esOrder: EsOrder): EsOrder {
        return esOperations.save(esOrder, entityDefinition.writeIndexCoordinates)
            .awaitFirst()
    }

    suspend fun saveAll(esOrders: List<EsOrder>): List<EsOrder> {
        return esOperations.saveAll(esOrders, entityDefinition.writeIndexCoordinates)
            .collectList().awaitFirst()
    }

    suspend fun deleteAll() {
        esOperations.delete(Query.findAll(), entityDefinition.writeIndexCoordinates)
            .awaitFirst()
    }

    suspend fun refresh() {
        val refreshRequest = RefreshRequest().indices(entityDefinition.aliasName, entityDefinition.writeAliasName)

        try {
            esOperations.execute { it.indices().refreshIndex(refreshRequest) }.awaitFirstOrNull()
        } catch (e: IOException) {
            throw RuntimeException(entityDefinition.writeAliasName + " refreshModifyIndex failed", e)
        }
    }
}
