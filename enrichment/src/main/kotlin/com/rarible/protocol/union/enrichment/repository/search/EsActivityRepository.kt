package com.rarible.protocol.union.enrichment.repository.search

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.logging.Logger
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
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.stereotype.Component

@Component
class EsActivityRepository(
    private val esOperations: ReactiveElasticsearchOperations,
    private val queryBuilderService: EsQueryBuilderService,
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
) {
    companion object {
        private val logger by Logger()
    }

    private val env = applicationEnvironmentInfo.name
    private val readIndexName = "protocol_union_${env}_activity"
    private val writeIndexName = "protocol_union_${env}_activity_write"
    private val readIndex = IndexCoordinates.of(readIndexName)
    private val writeIndexes = IndexCoordinates.of(readIndexName, writeIndexName)

    suspend fun findById(id: String): EsActivity? {
        return  esOperations.get(id, EsActivity::class.java, readIndex).awaitFirstOrNull()
    }

    suspend fun save(esActivity: EsActivity): EsActivity {
        return esOperations.save(esActivity, writeIndexes).awaitFirst()
    }

    suspend fun saveAll(esActivities: List<EsActivity>): List<EsActivity> {
        return esOperations.saveAll(esActivities, writeIndexes).collectList().awaitFirst()
    }

    suspend fun deleteAll() {
        if (env != "test") {
            logger.error("deleteAll() on non-test environment is forbidden")
            return
        }
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
