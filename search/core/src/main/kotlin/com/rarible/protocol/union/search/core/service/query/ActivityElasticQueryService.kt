package com.rarible.protocol.union.search.core.service.query

import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.search.core.ElasticActivity
import com.rarible.protocol.union.search.core.model.ActivityCursor.Companion.fromActivity
import com.rarible.protocol.union.search.core.model.ActivityQueryResult
import com.rarible.protocol.union.search.core.model.ActivitySort
import com.rarible.protocol.union.search.core.model.ElasticActivityFilter
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.stereotype.Service

@Service
class ActivityElasticQueryService(
    private val esOperations: ReactiveElasticsearchOperations,
    private val queryBuilderService: QueryBuilderService,
) {

    suspend fun query(filter: ElasticActivityFilter, sort: ActivitySort, limit: Int?): ActivityQueryResult {
        val query = queryBuilderService.build(filter, sort)
        query.maxResults = PageSize.ACTIVITY.limit(limit)

        val activities = esOperations.search(query, ElasticActivity::class.java)
            .collectList()
            .awaitFirst()
            .map { it.content }

        val cursor = if (activities.isEmpty()) {
            null
        } else {
            activities.last().fromActivity().toString()
        }

        return ActivityQueryResult(
            activities = activities,
            cursor = cursor
        )
    }
}
