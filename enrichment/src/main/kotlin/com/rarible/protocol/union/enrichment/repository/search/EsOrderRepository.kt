package com.rarible.protocol.union.enrichment.repository.search

import com.rarible.protocol.union.core.model.*
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.stereotype.Service

@Service
class EsOrderRepository(
    private val esOperations: ReactiveElasticsearchOperations
) {
    suspend fun save(esActivity: EsOrder): EsOrder {
        return esOperations.save(esActivity).awaitFirst()
    }

    suspend fun saveAll(esActivities: List<EsOrder>): List<EsOrder> {
        return esOperations.saveAll(esActivities, EsOrder::class.java).collectList().awaitFirst()
    }

    suspend fun deleteAll() {
        esOperations.delete(Query.findAll(), EsOrder::class.java).awaitFirst()
    }
}
