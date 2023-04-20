package com.rarible.protocol.union.enrichment.repository.search

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.model.elastic.EsCollection
import com.rarible.protocol.union.core.model.elastic.EsCollectionFilter
import com.rarible.protocol.union.core.model.elastic.EsCollectionLite
import com.rarible.protocol.union.core.util.PageSize
import com.rarible.protocol.union.enrichment.repository.search.internal.EsCollectionQueryBuilderService
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.stereotype.Component
import kotlin.system.measureTimeMillis

@Component
class EsCollectionRepository(
    private val queryBuilderService: EsCollectionQueryBuilderService,
    objectMapper: ObjectMapper,
    elasticsearchConverter: ElasticsearchConverter,
    esOperations: ReactiveElasticsearchOperations,
    elasticClient: ReactiveElasticsearchClient,
    esNameResolver: EsNameResolver
) : ElasticSearchRepository<EsCollection>(
    objectMapper,
    esOperations,
    esNameResolver.createEntityDefinitionExtended(EsCollection.ENTITY_DEFINITION),
    elasticsearchConverter,
    elasticClient,
    EsCollection::class.java,
    EsCollection::collectionId.name,
) {

    override fun entityId(entity: EsCollection): String {
        return entity.collectionId
    }

    suspend fun search(filter: EsCollectionFilter, limit: Int?): List<EsCollectionLite> {
        val query = queryBuilderService.build(filter)
        query.maxResults = PageSize.COLLECTION.limit(limit)
        query.trackTotalHits = false
        var result: List<EsCollectionLite>
        val time = measureTimeMillis {
            result = search(query)
        }
        logger.info("Collection search elapsed time: ${time}ms. Filter: $filter, limit: $limit")
        return result
    }

    suspend fun search(query: NativeSearchQuery): List<EsCollectionLite> {
        return esOperations.search(query, EsCollectionLite::class.java, entityDefinition.searchIndexCoordinates)
            .collectList()
            .awaitFirst()
            .map { it.content }
    }

    companion object {
        val logger by Logger()
    }
}
