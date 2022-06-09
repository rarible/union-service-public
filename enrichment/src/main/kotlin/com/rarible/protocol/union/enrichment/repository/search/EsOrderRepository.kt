package com.rarible.protocol.union.enrichment.repository.search

import com.rarible.protocol.union.core.elasticsearch.EsHelper
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.elasticsearch.EsRepository
import com.rarible.protocol.union.core.model.EsOrder
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.stereotype.Component
import java.io.IOException
import javax.annotation.PostConstruct

@Component
class EsOrderRepository(
    private val esOperations: ReactiveElasticsearchOperations, esNameResolver: EsNameResolver
) : EsRepository {
    val entityDefinition = esNameResolver.createEntityDefinitionExtended(EsOrder.ENTITY_DEFINITION)
    var brokenEsState: Boolean = true

    @PostConstruct
    override fun init() = runBlocking {
        brokenEsState = try {
            !EsHelper.existsIndexesForEntity(esOperations, entityDefinition.indexRootName)
        } catch (_: Exception) {
            true
        }
    }

    suspend fun findById(id: String): EsOrder? {
        return esOperations.get(id, EsOrder::class.java, entityDefinition.searchIndexCoordinates).awaitFirstOrNull()
    }

    suspend fun save(esOrder: EsOrder): EsOrder {
        if (brokenEsState) {
            throw IllegalStateException("No indexes to save")
        }
        return esOperations.save(esOrder, entityDefinition.writeIndexCoordinates).awaitFirst()
    }

    suspend fun saveAll(esOrders: List<EsOrder>): List<EsOrder> {
        if (brokenEsState) {
            throw IllegalStateException("No indexes to save")
        }
        return esOperations.saveAll(esOrders, entityDefinition.writeIndexCoordinates).collectList().awaitFirst()
    }

    override suspend fun refresh() {
        val refreshRequest = RefreshRequest().indices(entityDefinition.aliasName, entityDefinition.writeAliasName)

        try {
            esOperations.execute { it.indices().refreshIndex(refreshRequest) }.awaitFirstOrNull()
        } catch (e: IOException) {
            throw RuntimeException(entityDefinition.writeAliasName + " refreshModifyIndex failed", e)
        }
    }
}
