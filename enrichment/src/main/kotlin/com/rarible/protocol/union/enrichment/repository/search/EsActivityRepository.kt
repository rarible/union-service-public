package com.rarible.protocol.union.enrichment.repository.search

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.model.elastic.ElasticActivityFilter
import com.rarible.protocol.union.core.model.elastic.EsActivity
import com.rarible.protocol.union.core.model.elastic.EsActivityCursor.Companion.fromActivity
import com.rarible.protocol.union.core.model.elastic.EsActivityQueryResult
import com.rarible.protocol.union.core.model.elastic.EsActivitySort
import com.rarible.protocol.union.core.util.PageSize
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.enrichment.repository.search.internal.EsActivityQueryBuilderService
import com.rarible.protocol.union.enrichment.repository.search.internal.mustMatchRange
import com.rarible.protocol.union.enrichment.repository.search.internal.mustMatchTerm
import kotlinx.coroutines.reactive.awaitFirst
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.data.elasticsearch.core.ElasticsearchAggregation
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class EsActivityRepository(
    private val queryBuilderService: EsActivityQueryBuilderService,
    objectMapper: ObjectMapper,
    elasticsearchConverter: ElasticsearchConverter,
    esOperations: ReactiveElasticsearchOperations,
    elasticClient: ReactiveElasticsearchClient,
    esNameResolver: EsNameResolver
) : ElasticSearchRepository<EsActivity>(
    objectMapper,
    esOperations,
    esNameResolver.createEntityDefinitionExtended(EsActivity.ENTITY_DEFINITION),
    elasticsearchConverter,
    elasticClient,
    EsActivity::class.java,
    EsActivity::activityId.name,
) {

    override fun entityId(entity: EsActivity): String {
        return entity.activityId
    }

    suspend fun search(
        filter: ElasticActivityFilter,
        sort: EsActivitySort,
        limit: Int?
    ): EsActivityQueryResult {
        val query = queryBuilderService.build(filter, sort)
        query.maxResults = PageSize.ACTIVITY.limit(limit)
        query.trackTotalHits = false

        return logIfSlow(filter, query.query, query.elasticsearchSorts) {
            logger.info("ES Activity query: ${query.query}, filter: ${query.filter}, sort: ${query.elasticsearchSorts}")
            val result = search(query)
            logger.info("Result: $result")
            result
        }
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

    suspend fun findTradedDistinctCollections(
        blockchain: BlockchainDto,
        since: Instant,
        limit: Int
    ): List<CollectionIdDto> {
        val boolQuery = BoolQueryBuilder()
        boolQuery.mustMatchTerm(blockchain.name, EsActivity::blockchain.name)
        boolQuery.mustMatchRange(since, null, EsActivity::date.name)
        val query = NativeSearchQueryBuilder()
            .withQuery(boolQuery)
            .withAggregations(
                AggregationBuilders
                    .terms("collections")
                    .field("collection")
                    .size(limit)
            )
            .build()
        query.maxResults = 0
        val aggregation = esOperations.aggregate(query, EsActivity::class.java, entityDefinition.searchIndexCoordinates)
            .map { (it as ElasticsearchAggregation).aggregation() }
            .awaitFirst()
        if (aggregation !is ParsedStringTerms) {
            logger.warn("Unknown aggregation type {}", aggregation.type)
            return emptyList()
        }
        return aggregation.buckets
            .map { CollectionIdDto(blockchain, it.key as String) }
    }
}
