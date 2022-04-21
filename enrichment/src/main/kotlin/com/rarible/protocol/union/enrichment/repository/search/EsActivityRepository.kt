package com.rarible.protocol.union.enrichment.repository.search

import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.model.ElasticActivityFilter
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.EsActivityCursor.Companion.fromActivity
import com.rarible.protocol.union.core.model.EsActivityQueryResult
import com.rarible.protocol.union.core.model.EsActivitySort
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.enrichment.repository.search.internal.EsQueryBuilderService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class EsActivityRepository(
    private val elasticClient: RestHighLevelClient,
    private val esOperations: ReactiveElasticsearchOperations,
    private val queryBuilderService: EsQueryBuilderService,
    esNameResolver: EsNameResolver
) {

    val entityDefinition = esNameResolver.createEntityDefinitionExtended(EsActivity.ENTITY_DEFINITION)

    suspend fun findById(id: String): EsActivity? {
        return esOperations.get(id, EsActivity::class.java, entityDefinition.searchIndexCoordinates).awaitFirstOrNull()
    }

    suspend fun save(esActivity: EsActivity): EsActivity {
        return esOperations.save(esActivity, entityDefinition.writeIndexCoordinates)
            .awaitFirst()
    }

    suspend fun saveAll(esActivities: List<EsActivity>): List<EsActivity> {
        return esOperations.saveAll(esActivities, entityDefinition.writeIndexCoordinates)
            .collectList().awaitFirst()
    }

    /**
     * For tests only
     */
    suspend fun deleteAll() {
        esOperations.delete(
            Query.findAll(),
            Any::class.java,
            entityDefinition.writeIndexCoordinates
        ).awaitFirstOrNull()
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
        val activities = esOperations.search(query, EsActivity::class.java, entityDefinition.searchIndexCoordinates)
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

    fun refresh() {
        val refreshRequest = RefreshRequest().indices(entityDefinition.aliasName, entityDefinition.writeAliasName)

        try {
            elasticClient.indices().refresh(refreshRequest, RequestOptions.DEFAULT)
        } catch (e: IOException) {
            throw RuntimeException(entityDefinition.writeAliasName + " refreshModifyIndex failed", e)
        }
    }
}
