package com.rarible.protocol.union.enrichment.repository.search

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.model.ElasticItemFilter
import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsItemSort
import com.rarible.protocol.union.core.model.EsQueryResult
import com.rarible.protocol.union.dto.continuation.page.ArgSlice
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.enrichment.repository.search.internal.EsItemBuilderService.buildQuery
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.DB)
class EsItemRepository(
    objectMapper: ObjectMapper,
    elasticsearchConverter: ElasticsearchConverter,
    esOperations: ReactiveElasticsearchOperations,
    elasticClient: ReactiveElasticsearchClient,
    esNameResolver: EsNameResolver
) : ElasticSearchRepository<EsItem>(
    objectMapper,
    esOperations,
    esNameResolver.createEntityDefinitionExtended(EsItem.ENTITY_DEFINITION),
    elasticsearchConverter,
    elasticClient,
    EsItem::class.java,
    EsItem::itemId.name,
    EsItem::itemId
) {

    suspend fun search(
        filter: ElasticItemFilter, sort: EsItemSort, limit: Int?
    ): EsQueryResult<EsItem> {
        val query = filter.buildQuery(sort)
        query.maxResults = PageSize.ITEM.limit(limit)
        return search(query)
    }

    suspend fun search(query: NativeSearchQuery): EsQueryResult<EsItem> {

        val hits = esOperations.search(query, EsItem::class.java, entityDefinition.searchIndexCoordinates).collectList()
            .awaitFirst()
        val content = hits.map { it.content }

        val last = hits.lastOrNull()
        val continuationString = if (last != null && last.sortValues.size > 0) {
            objectMapper.writeValueAsString(
                hits.last().sortValues.last()
            )
        } else ArgSlice.COMPLETED

        return EsQueryResult(
            content = content, cursor = continuationString
        )
    }
}
