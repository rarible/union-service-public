package com.rarible.protocol.union.enrichment.repository.search

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.model.elastic.EsOwnership
import com.rarible.protocol.union.core.model.elastic.EsOwnershipFilter
import com.rarible.protocol.union.core.model.elastic.EsOwnershipSort
import com.rarible.protocol.union.core.util.PageSize
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.repository.search.internal.EsEntitySearchAfterCursorService
import com.rarible.protocol.union.enrichment.repository.search.internal.EsOwnershipQueryBuilderService
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter
import org.springframework.stereotype.Component

@Component
class EsOwnershipRepository(
    private val queryBuilderService: EsOwnershipQueryBuilderService,
    private val queryCursorService: EsEntitySearchAfterCursorService,
    objectMapper: ObjectMapper,
    elasticsearchConverter: ElasticsearchConverter,
    esOperations: ReactiveElasticsearchOperations,
    elasticClient: ReactiveElasticsearchClient,
    esNameResolver: EsNameResolver
) : ElasticSearchRepository<EsOwnership>(
    objectMapper,
    esOperations,
    esNameResolver.createEntityDefinitionExtended(EsOwnership.ENTITY_DEFINITION),
    elasticsearchConverter,
    elasticClient,
    EsOwnership::class.java,
    EsOwnership::ownershipId.name,
) {

    override fun entityId(entity: EsOwnership): String {
        return entity.ownershipId
    }

    suspend fun search(
        filter: EsOwnershipFilter,
        sort: EsOwnershipSort = EsOwnershipSort.DEFAULT,
        limit: Int? = null
    ): Slice<EsOwnership> {
        val query = queryBuilderService.build(filter, sort)
        query.maxResults = PageSize.OWNERSHIP.limit(limit)
        query.trackTotalHits = false

        val searchHits = logIfSlow(filter, sort, query) {
            esOperations
                .search(query, EsOwnership::class.java, entityDefinition.searchIndexCoordinates)
                .collectList().awaitFirst()
        }
        val cursor = queryCursorService.buildCursor(searchHits.lastOrNull())

        return Slice(
            continuation = cursor,
            entities = searchHits.map { it.content },
        )
    }
}
