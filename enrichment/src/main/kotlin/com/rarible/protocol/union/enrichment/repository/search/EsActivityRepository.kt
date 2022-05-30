package com.rarible.protocol.union.enrichment.repository.search

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.model.ElasticActivityFilter
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.EsActivityCursor.Companion.fromActivityLite
import com.rarible.protocol.union.core.model.EsActivityLite
import com.rarible.protocol.union.core.model.EsActivityQueryResult
import com.rarible.protocol.union.core.model.EsActivitySort
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.enrichment.repository.search.internal.EsActivityQueryBuilderService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.stereotype.Component
import java.io.IOException

@Component
@CaptureSpan(type = SpanType.DB)
class EsActivityRepository(
    private val esOperations: ReactiveElasticsearchOperations,
    private val queryBuilderService: EsActivityQueryBuilderService,
    esNameResolver: EsNameResolver
) : EsRepository {
    val entityDefinition = esNameResolver.createEntityDefinitionExtended(EsActivity.ENTITY_DEFINITION)

    suspend fun findById(id: String): EsActivity? {
        return esOperations.get(id, EsActivity::class.java, entityDefinition.searchIndexCoordinates).awaitFirstOrNull()
    }

    suspend fun save(esActivity: EsActivity): EsActivity {
        return esOperations.save(esActivity, entityDefinition.writeIndexCoordinates).awaitFirst()
    }

    suspend fun saveAll(esActivities: List<EsActivity>): List<EsActivity> {
        return saveAllToIndex(esActivities, entityDefinition.writeIndexCoordinates)
    }

    suspend fun saveAll(esActivities: List<EsActivity>, indexName: String?): List<EsActivity> {
        return if (indexName == null) {
            saveAll(esActivities)
        } else {
            saveAllToIndex(esActivities, IndexCoordinates.of(indexName))
        }
    }

    private suspend fun saveAllToIndex(esActivities: List<EsActivity>, index: IndexCoordinates): List<EsActivity> {
        return esOperations
            .saveAll(esActivities, index)
            .collectList()
            .awaitFirst()
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
        val activities = esOperations.search(query, EsActivityLite::class.java, entityDefinition.searchIndexCoordinates)
            .collectList()
            .awaitFirst()
            .map { it.content }

        val cursor = if (activities.isEmpty()) {
            null
        } else {
            activities.last().fromActivityLite().toString()
        }

        return EsActivityQueryResult(
            activities = activities,
            cursor = cursor
        )
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
