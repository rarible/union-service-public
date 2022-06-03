package com.rarible.protocol.union.enrichment.repository.search

import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.core.model.EsOwnershipFilter
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.Criteria
import org.springframework.data.elasticsearch.core.query.CriteriaQuery
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class EsOwnershipRepository(
    private val esOperations: ReactiveElasticsearchOperations, esNameResolver: EsNameResolver
) : EsRepository {
    val entityDefinition = esNameResolver.createEntityDefinitionExtended(EsOwnership.ENTITY_DEFINITION)

    suspend fun findById(id: String): EsOwnership? {
        return esOperations.get(id, EsOwnership::class.java, entityDefinition.searchIndexCoordinates).awaitFirstOrNull()
    }

    suspend fun findByFilter(filter: EsOwnershipFilter): List<EsOwnership> {
        val query = filter.asQuery()
        return esOperations.search(query, EsOwnership::class.java, entityDefinition.searchIndexCoordinates)
            .collectList().awaitFirst().map { it.content }
    }

    suspend fun saveAll(esOwnerships: Collection<EsOwnership>, indexName: String? = null): List<EsOwnership> {
        return esOperations.saveAll(esOwnerships, index(indexName)).collectList().awaitFirst()
    }

    suspend fun deleteAll(ownershipIds: List<String>) {
        val query = CriteriaQuery(Criteria(EsOwnership::ownershipId.name).`in`(ownershipIds))
        esOperations.delete(
            query, EsOwnership::class.java, entityDefinition.writeIndexCoordinates
        ).awaitFirstOrNull()
    }

    override suspend fun refresh() {
        val refreshRequest = RefreshRequest().indices(entityDefinition.aliasName, entityDefinition.writeAliasName)

        try {
            esOperations.execute { it.indices().refreshIndex(refreshRequest) }.awaitFirstOrNull()
        } catch (e: IOException) {
            throw RuntimeException(entityDefinition.writeAliasName + " refreshModifyIndex failed", e)
        }
    }

    private fun index(indexName: String?) = indexName
        ?.let { IndexCoordinates.of(it) } ?: entityDefinition.writeIndexCoordinates
}
