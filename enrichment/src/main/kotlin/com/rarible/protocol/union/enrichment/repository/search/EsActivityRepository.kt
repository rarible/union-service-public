package com.rarible.protocol.union.enrichment.repository.search

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.elasticsearch.EsHelper
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.elasticsearch.EsRepository
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
import kotlinx.coroutines.runBlocking
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest
import org.elasticsearch.index.query.TermsQueryBuilder
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.stereotype.Component
import java.io.IOException
import javax.annotation.PostConstruct

@Component
@CaptureSpan(type = SpanType.DB)
class EsActivityRepository(
    private val esOperations: ReactiveElasticsearchOperations,
    private val queryBuilderService: EsActivityQueryBuilderService,
    esNameResolver: EsNameResolver
) : EsRepository {
    val entityDefinition = esNameResolver.createEntityDefinitionExtended(EsActivity.ENTITY_DEFINITION)

    var brokenEsState: Boolean = true

    @PostConstruct
    override fun init() = runBlocking {
        brokenEsState = try {
            !EsHelper.existsIndexesForEntity(esOperations, entityDefinition.indexRootName)
        } catch (_: Exception) {
            true
        }
    }

    suspend fun findById(id: String): EsActivity? {
        return esOperations.get(id, EsActivity::class.java, entityDefinition.searchIndexCoordinates).awaitFirstOrNull()
    }

    suspend fun save(esActivity: EsActivity): EsActivity {
        if (brokenEsState) {
            throw IllegalStateException("No indexes to save")
        }
        return esOperations.save(esActivity, entityDefinition.writeIndexCoordinates).awaitFirst()
    }

    suspend fun saveAll(esActivities: List<EsActivity>): List<EsActivity> {
        if (brokenEsState) {
            throw IllegalStateException("No indexes to save")
        }
        return saveAllToIndex(esActivities, entityDefinition.writeIndexCoordinates)
    }

    suspend fun saveAll(esActivities: List<EsActivity>, indexName: String?): List<EsActivity> {
        if (brokenEsState) {
            throw IllegalStateException("No indexes to save")
        }
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

    suspend fun delete(activityIds: List<String>): Long? {
        val query = NativeSearchQueryBuilder()
            .withQuery(TermsQueryBuilder(EsActivity::activityId.name, activityIds))
            .build()
        return esOperations.delete(
            query,
            Any::class.java,
            entityDefinition.writeIndexCoordinates
        ).awaitFirstOrNull()?.deleted
    }

    suspend fun search(
        filter: ElasticActivityFilter,
        sort: EsActivitySort,
        limit: Int?
    ): EsActivityQueryResult {
        val query = queryBuilderService.build(filter, sort)
        query.maxResults = PageSize.ACTIVITY.limit(limit)
        query.trackTotalHits = false

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
