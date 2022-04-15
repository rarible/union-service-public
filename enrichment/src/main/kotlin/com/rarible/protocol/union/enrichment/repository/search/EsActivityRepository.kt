package com.rarible.protocol.union.enrichment.repository.search

import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.EsActivityCursor.Companion.fromActivity
import com.rarible.protocol.union.core.model.EsActivityQueryResult
import com.rarible.protocol.union.core.model.EsActivitySort
import com.rarible.protocol.union.core.model.ElasticActivityFilter
import com.rarible.protocol.union.enrichment.repository.search.internal.EsQueryBuilderService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.stereotype.Component

@Component
class EsActivityRepository(
    private val esOperations: ReactiveElasticsearchOperations,
    private val queryBuilderService: EsQueryBuilderService,
) {
    suspend fun findById(id: String): EsActivity? {
        return  esOperations.get(id, EsActivity::class.java).awaitFirstOrNull()
    }

    suspend fun save(esActivity: EsActivity): EsActivity {
        return esOperations.save(esActivity).awaitFirst()
    }

    suspend fun saveAll(esActivities: List<EsActivity>): List<EsActivity> {
        return esOperations.saveAll(esActivities, EsActivity::class.java).collectList().awaitFirst()
    }

    suspend fun deleteAll() {
        esOperations.delete(Query.findAll(), EsActivity::class.java).awaitFirst()
    }

    suspend fun search(
        filter: ElasticActivityFilter,
        sort: EsActivitySort,
        limit: Int?
    ): EsActivityQueryResult {
        val query = queryBuilderService.build(filter, sort)
        query.maxResults = PageSize.ACTIVITY.limit(limit)

        val activities = esOperations.search(query, EsActivity::class.java)
            .collectList()
            .awaitFirst()
            .map { it.content }

        val cursor = if (activities.isEmpty()) {
            null
        } else {
            activities.last().fromActivity().toString()
        }

        return EsActivityQueryResult(
            activities = activities,
            cursor = cursor
        )
    }
}
