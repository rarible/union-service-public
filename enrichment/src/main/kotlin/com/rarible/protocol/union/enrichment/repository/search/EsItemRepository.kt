package com.rarible.protocol.union.enrichment.repository.search

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.model.ElasticItemFilter
import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsItemQueryResult
import com.rarible.protocol.union.core.model.EsItemSort
import com.rarible.protocol.union.dto.continuation.page.ArgSlice
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.enrichment.repository.search.internal.EsItemBuilderService
import com.rarible.protocol.union.enrichment.repository.search.internal.EsItemQuerySortService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.stereotype.Component
import java.io.IOException

@Component
@CaptureSpan(type = SpanType.DB)
class EsItemRepository(
    private val objectMapper: ObjectMapper,
    private val client: ReactiveElasticsearchClient,
    private val esOperations: ReactiveElasticsearchOperations,
    private val esItemBuilderService: EsItemBuilderService,
    private val esItemQuerySortService: EsItemQuerySortService,
    esNameResolver: EsNameResolver
) {
    val entityDefinition = esNameResolver.createEntityDefinitionExtended(EsItem.ENTITY_DEFINITION)

    suspend fun findById(id: String): EsItem? {
        return esOperations.get(id, EsItem::class.java, entityDefinition.searchIndexCoordinates).awaitFirstOrNull()
    }

    suspend fun save(esItem: EsItem): EsItem {
        return esOperations.save(esItem, entityDefinition.writeIndexCoordinates).awaitFirst()
    }

    suspend fun saveAll(esItems: List<EsItem>): List<EsItem> {
        return saveAllToIndex(esItems, entityDefinition.writeIndexCoordinates)
    }

    suspend fun saveAll(esItems: List<EsItem>, indexName: String?): List<EsItem> {
        return if (indexName == null) {
            saveAll(esItems)
        } else {
            saveAllToIndex(esItems, IndexCoordinates.of(indexName))
        }
    }

    private suspend fun saveAllToIndex(esItems: List<EsItem>, index: IndexCoordinates): List<EsItem> {
        return esOperations
            .saveAll(esItems, index)
            .collectList()
            .awaitFirst()
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
        filter: ElasticItemFilter,
        sort: EsItemSort,
        limit: Int?
    ): EsItemQueryResult {
        val boolQuery = esItemBuilderService.build2(filter)
        val sourceBuilder = SearchSourceBuilder().query(boolQuery).size(PageSize.ITEM.limit(limit))
        esItemQuerySortService.applySort(sourceBuilder, sort)
        filter.cursor?.let { sourceBuilder.searchAfter(arrayOf(it)) }

        val searchResponse = client.searchForResponse(
            SearchRequest().indices(entityDefinition.searchIndexCoordinates.indexName)
                .source(sourceBuilder)
        ).awaitFirst()

        val hits = searchResponse.hits
        val items = hits.hits.map { objectMapper.readValue(it.sourceAsString, EsItem::class.java) }

        val continuationString = if ((hits.lastOrNull()?.sortValues?.size ?: 0) > 0) {
            objectMapper.writeValueAsString(
                hits.last().sortValues.toList()
            )
        } else ArgSlice.COMPLETED

        return EsItemQueryResult(
            items = items,
            continuation = continuationString
        )
    }

    suspend fun search(query: NativeSearchQuery): EsItemQueryResult {

        val hits = esOperations.search(query, EsItem::class.java, entityDefinition.searchIndexCoordinates)
            .collectList()
            .awaitFirst()
        val items = hits.map { it.content }

        val continuationString = if (hits.last().sortValues.size > 0) {
            objectMapper.writeValueAsString(
                hits.last().sortValues.toList()
            )
        } else ArgSlice.COMPLETED

        return EsItemQueryResult(
            items = items,
            continuation = continuationString
        )
    }

    suspend fun refresh() {
        val refreshRequest = RefreshRequest().indices(entityDefinition.aliasName, entityDefinition.writeAliasName)

        try {
            esOperations.execute { it.indices().refreshIndex(refreshRequest) }.awaitFirstOrNull()
        } catch (e: IOException) {
            throw RuntimeException(entityDefinition.writeAliasName + " refreshModifyIndex failed", e)
        }
    }
}
