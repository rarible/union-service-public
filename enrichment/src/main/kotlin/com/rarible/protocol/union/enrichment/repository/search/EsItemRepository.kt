package com.rarible.protocol.union.enrichment.repository.search

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.model.elastic.EsItem
import com.rarible.protocol.union.core.model.elastic.EsItemFilter
import com.rarible.protocol.union.core.model.elastic.EsItemLite
import com.rarible.protocol.union.core.model.elastic.EsItemSort
import com.rarible.protocol.union.core.util.PageSize
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.repository.search.internal.EsEntitySearchAfterCursorService
import com.rarible.protocol.union.enrichment.repository.search.internal.EsItemQueryBuilderService
import kotlinx.coroutines.reactive.awaitFirst
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.QueryBuilders.existsQuery
import org.elasticsearch.index.query.QueryBuilders.functionScoreQuery
import org.elasticsearch.index.query.QueryBuilders.termQuery
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms
import org.elasticsearch.search.aggregations.metrics.ParsedTopHits
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHit
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter
import org.springframework.data.elasticsearch.core.document.DocumentAdapters
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.stereotype.Component

@Component
class EsItemRepository(
    private val queryBuilderService: EsItemQueryBuilderService,
    private val queryCursorService: EsEntitySearchAfterCursorService,
    private val elasticsearchConverter: ElasticsearchConverter,
    objectMapper: ObjectMapper,
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
) {

    override fun entityId(entity: EsItem): String {
        return entity.id
    }

    suspend fun search(filter: EsItemFilter, sort: EsItemSort, limit: Int?): Slice<EsItemLite> {
        val query = queryBuilderService.build(filter, sort)

        query.maxResults = PageSize.ITEM.limit(limit)
        query.trackTotalHits = false

        query.addSourceFilter(FetchSourceFilter(EsItemLite.FIELDS, null))
        val searchHits = logIfSlow(filter, query) {
            search(query)
        }
        val cursor = queryCursorService.buildCursor(searchHits.lastOrNull())

        return Slice(
            continuation = cursor,
            entities = searchHits.map { it.content },
        )
    }

    suspend fun search(query: NativeSearchQuery): List<SearchHit<EsItemLite>> {
        return esOperations.search(query, EsItemLite::class.java, entityDefinition.searchIndexCoordinates)
            .collectList()
            .awaitFirst()
    }

    suspend fun countItemsInCollection(collectionId: String): Long {
        val query = NativeSearchQuery(termQuery(EsItem::collection.name, collectionId))

        return esOperations
            .count(
                query,
                EsItem::class.java,
                entityDefinition.searchIndexCoordinates
            )
            .awaitFirst()
    }

    suspend fun getRandomItemsFromCollection(collectionId: String, size: Int): List<EsItem> {
        val query = NativeSearchQuery(
            functionScoreQuery(
                termQuery(EsItem::collection.name, collectionId),
                ScoreFunctionBuilders.randomFunction().seed(System.currentTimeMillis()).setField("_seq_no")
            )
        )
        return esOperations
            .search(
                query,
                EsItem::class.java,
                entityDefinition.searchIndexCoordinates
            )
            .map {
                it.content
            }
            .collectList()
            .awaitFirst()
    }

    suspend fun getCheapestItems(collectionId: String): List<EsItem> {
        val aggregation = AggregationBuilders
            .terms(EsItem::bestSellCurrency.name)
            .field(EsItem::bestSellCurrency.name)
            .size(1000)
            .subAggregation(
                AggregationBuilders.topHits("item")
                    .fetchSource(true)
                    .size(1)
                    .sort(EsItem::bestSellAmount.name)
            )
        val filter = QueryBuilders.boolQuery()
            .filter(
                QueryBuilders.boolQuery()
                    .must(termQuery(EsItem::collection.name, collectionId))
                    .must(existsQuery(EsItem::bestSellAmount.name))
                    .must(existsQuery(EsItem::bestSellCurrency.name))
            )

        val query = NativeSearchQuery(filter)
        query.addAggregation(aggregation)
        return esOperations
            .aggregate(
                query,
                EsItem::class.java,
                entityDefinition.searchIndexCoordinates,
            ).map {
                (it.aggregation() as ParsedStringTerms).buckets.map {
                    val hit = (it.aggregations.get("item") as ParsedTopHits).hits.first()
                    elasticsearchConverter.read(EsItem::class.java, DocumentAdapters.from(hit))
                }
            }
            .collectList()
            .awaitFirst()
            .flatten()
    }
}
