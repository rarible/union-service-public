package com.rarible.protocol.union.enrichment.repository.search

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.model.ElasticItemFilter
import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsItemQueryResult
import com.rarible.protocol.union.core.model.EsItemSort
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.enrichment.repository.search.internal.EsItemBuilderService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.stereotype.Component
import java.io.IOException

@Component
@CaptureSpan(type = SpanType.DB)
class EsItemRepository(
    private val esOperations: ReactiveElasticsearchOperations,
    private val esItemBuilderService: EsItemBuilderService,
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
        val query = esItemBuilderService.build(filter, sort)
        query.maxResults = PageSize.ITEM.limit(limit)

        return search(query)
    }

    suspend fun search(query: NativeSearchQuery): EsItemQueryResult {
        val items = esOperations.search(query, EsItem::class.java, entityDefinition.searchIndexCoordinates)
            .collectList()
            .awaitFirst()
            .map { it.content }

        val cursor = if (items.isEmpty()) {
            null
        } else {
            items.last().itemId
        }

        return EsItemQueryResult(
            items = items,
            cursor = cursor
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
