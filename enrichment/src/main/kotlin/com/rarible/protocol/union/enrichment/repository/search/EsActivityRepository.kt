package com.rarible.protocol.union.enrichment.repository.search

import com.rarible.protocol.union.core.EsIndexProvider
import com.rarible.protocol.union.core.model.ElasticActivityFilter
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.EsActivityCursor.Companion.fromActivity
import com.rarible.protocol.union.core.model.EsActivityQueryResult
import com.rarible.protocol.union.core.model.EsActivitySort
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.enrichment.repository.search.internal.EsQueryBuilderService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.stereotype.Component

@Component
class EsActivityRepository(
    private val esOperations: ReactiveElasticsearchOperations,
    private val queryBuilderService: EsQueryBuilderService,
    esIndexProvider: EsIndexProvider,
) {
    companion object {
        private const val ENTITY = "activity"
    }

    private val readIndex = esIndexProvider.getReadIndexCoords(ENTITY)
    private val writeIndexes = esIndexProvider.getWriteIndexCoords(ENTITY)

    suspend fun findById(id: String): EsActivity? {
        return esOperations.get(id, EsActivity::class.java, readIndex).awaitFirstOrNull()
    }

    suspend fun save(esActivity: EsActivity): EsActivity {
        return esOperations.save(esActivity, writeIndexes).awaitFirst()
    }

    suspend fun saveAll(esActivities: List<EsActivity>): List<EsActivity> {
        return esOperations.saveAll(esActivities, writeIndexes).collectList().awaitFirst()
    }

    /**
     * For tests only
     */
    suspend fun deleteAll() {
        esOperations.delete(Query.findAll(), Any::class.java, writeIndexes).awaitFirstOrNull()
        // TODO remove when we have ES Bootstrap for tests
        esOperations.delete(Query.findAll(), Any::class.java, readIndex).awaitFirstOrNull()
    }

    suspend fun search(
        filter: ElasticActivityFilter,
        sort: EsActivitySort,
        limit: Int?
    ): EsActivityQueryResult {
        val query = queryBuilderService.build(filter, sort)
        query.maxResults = PageSize.ACTIVITY.limit(limit)

        return search(query)
    }

    suspend fun search(query: NativeSearchQuery): EsActivityQueryResult {
        val activities = esOperations.search(query, EsActivity::class.java, readIndex)
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
