package com.rarible.protocol.union.core.elasticsearch.repository

import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.model.EsOwnership
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.Criteria
import org.springframework.data.elasticsearch.core.query.CriteriaQuery
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class EsOwnershipRepository(
    private val esOperations: ReactiveElasticsearchOperations,
    esNameResolver: EsNameResolver
) {
    val entityDefinition = esNameResolver.createEntityDefinitionExtended(EsOwnership.ENTITY_DEFINITION)

    suspend fun findById(id: String): EsOwnership? {
        return esOperations.get(id, EsOwnership::class.java, entityDefinition.searchIndexCoordinates).awaitFirstOrNull()
    }

    suspend fun saveAll(esOwnerships: List<EsOwnership>): List<EsOwnership> {
        return esOperations.saveAll(esOwnerships, entityDefinition.writeIndexCoordinates)
            .collectList().awaitFirst()
    }

    suspend fun deleteAll(ownershipIds: List<String>) {
        val query = CriteriaQuery(Criteria(EsOwnership::ownershipId.name).`in`(ownershipIds))
        esOperations.delete(
            query,
            EsOwnership::class.java,
            entityDefinition.writeIndexCoordinates
        ).awaitFirstOrNull()
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
