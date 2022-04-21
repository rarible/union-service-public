package com.rarible.protocol.union.enrichment.repository.search

import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.model.EsOrder
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class EsOrderRepository(
    private val elasticClient: RestHighLevelClient,
    private val esOperations: ReactiveElasticsearchOperations,
    esNameResolver: EsNameResolver
) {
    val entityDefinition = esNameResolver.createEntityDefinitionExtended(EsOrder.ENTITY_DEFINITION)

    suspend fun findById(id: String): EsOrder? {
        return esOperations.get(id, EsOrder::class.java, entityDefinition.searchIndexCoordinates).awaitFirstOrNull()
    }

    suspend fun save(esActivity: EsOrder): EsOrder {
        return esOperations.save(esActivity, entityDefinition.writeIndexCoordinates)
            .awaitFirst()
    }

    suspend fun saveAll(esActivities: List<EsOrder>): List<EsOrder> {
        return esOperations.saveAll(esActivities, entityDefinition.writeIndexCoordinates)
            .collectList().awaitFirst()
    }

    suspend fun deleteAll() {
        esOperations.delete(Query.findAll(), entityDefinition.writeIndexCoordinates)
            .awaitFirst()
    }

    fun refresh() {
        val refreshRequest = RefreshRequest().indices(entityDefinition.aliasName, entityDefinition.writeAliasName)

        try {
            elasticClient.indices().refresh(refreshRequest, RequestOptions.DEFAULT)
        } catch (e: IOException) {
            throw RuntimeException(entityDefinition.writeAliasName + " refreshModifyIndex failed", e)
        }
    }
}
