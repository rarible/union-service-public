package com.rarible.protocol.union.enrichment.repository.search

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.elasticsearch.EsHelper
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.elasticsearch.EsRepository
import com.rarible.protocol.union.core.model.ElasticItemFilter
import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsItemSort
import com.rarible.protocol.union.core.model.EsQueryResult
import com.rarible.protocol.union.dto.continuation.page.ArgSlice
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.enrichment.repository.search.internal.EsItemBuilderService.buildQuery
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.Criteria
import org.springframework.data.elasticsearch.core.query.CriteriaQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.stereotype.Component
import java.io.IOException
import javax.annotation.PostConstruct

@Component
@CaptureSpan(type = SpanType.DB)
class EsItemRepository(
    private val objectMapper: ObjectMapper,
    private val esOperations: ReactiveElasticsearchOperations,
    esNameResolver: EsNameResolver
) : EsRepository {
    val entityDefinition = esNameResolver.createEntityDefinitionExtended(EsItem.ENTITY_DEFINITION)
    var brokenEsState: Boolean = true

    @PostConstruct
    override fun init() = runBlocking {
        brokenEsState = try {
            !EsHelper.existsIndexesForEntity(esOperations, entityDefinition.indexRootName)
        } catch (_: Exception) {
            true
        }
    }

    suspend fun findById(id: String): EsItem? {
        return esOperations.get(id, EsItem::class.java, entityDefinition.searchIndexCoordinates).awaitFirstOrNull()
    }

    suspend fun save(esItem: EsItem): EsItem {
        if (brokenEsState) {
            throw IllegalStateException("No indexes to save")
        }
        return esOperations.save(esItem, entityDefinition.writeIndexCoordinates).awaitFirst()
    }

    suspend fun saveAll(esItems: List<EsItem>): List<EsItem> {
        if (brokenEsState) {
            throw IllegalStateException("No indexes to save")
        }
        return saveAllToIndex(esItems, entityDefinition.writeIndexCoordinates)
    }

    suspend fun saveAll(esItems: List<EsItem>, indexName: String?): List<EsItem> {
        if (brokenEsState) {
            throw IllegalStateException("No indexes to save")
        }
        return if (indexName == null) {
            saveAll(esItems)
        } else {
            saveAllToIndex(esItems, IndexCoordinates.of(indexName))
        }
    }

    suspend fun deleteAll(itemIds: List<String>) {
        val query = CriteriaQuery(Criteria(EsItem::itemId.name).`in`(itemIds))
        esOperations.delete(
            query, EsItem::class.java, entityDefinition.writeIndexCoordinates
        ).awaitFirstOrNull()
    }

    private suspend fun saveAllToIndex(esItems: List<EsItem>, index: IndexCoordinates): List<EsItem> {
        return esOperations
            .saveAll(esItems, index)
            .collectList()
            .awaitFirst()
    }

    suspend fun search(
        filter: ElasticItemFilter,
        sort: EsItemSort,
        limit: Int?
    ): EsQueryResult<EsItem> {
        val query = filter.buildQuery(sort)
        query.maxResults = PageSize.ITEM.limit(limit)
        return search(query)
    }

    suspend fun search(query: NativeSearchQuery): EsQueryResult<EsItem> {

        val hits = esOperations.search(query, EsItem::class.java, entityDefinition.searchIndexCoordinates)
            .collectList()
            .awaitFirst()
        val content = hits.map { it.content }

        val last = hits.lastOrNull()
        val continuationString = if (last != null && last.sortValues.size > 0) {
            objectMapper.writeValueAsString(
                hits.last().sortValues.last()
            )
        } else ArgSlice.COMPLETED

        return EsQueryResult(
            content = content,
            cursor = continuationString
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
