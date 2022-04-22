package com.rarible.protocol.union.enrichment.repository.search

import com.rarible.protocol.union.core.model.EsOwnership
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.Criteria
import org.springframework.data.elasticsearch.core.query.CriteriaQuery
import org.springframework.stereotype.Component

@Component
class EsOwnershipRepository(
    private val esOperations: ReactiveElasticsearchOperations,
) {
    suspend fun findById(id: String): EsOwnership? {
        return esOperations.get(id, EsOwnership::class.java).awaitFirstOrNull()
    }

    suspend fun saveAll(esOwnerships: List<EsOwnership>): List<EsOwnership> {
        return esOperations.saveAll(esOwnerships, EsOwnership::class.java).collectList().awaitFirst()
    }

    suspend fun deleteAll(ownershipIds: List<String>) {
        val query = CriteriaQuery(Criteria(EsOwnership::ownershipId.name).`in`(ownershipIds))
        esOperations.delete(query, EsOwnership::class.java).awaitFirstOrNull()
    }
}
