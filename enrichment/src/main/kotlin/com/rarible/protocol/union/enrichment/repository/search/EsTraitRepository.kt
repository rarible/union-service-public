package com.rarible.protocol.union.enrichment.repository.search

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.model.elastic.EsTrait
import com.rarible.protocol.union.core.model.elastic.EsTraitFilter
import com.rarible.protocol.union.core.model.trait.Trait
import com.rarible.protocol.union.core.model.trait.TraitEntry
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.aggregations.Aggregation
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms
import org.elasticsearch.search.aggregations.metrics.ParsedTopHits
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter
import org.springframework.stereotype.Component

@Component
class EsTraitRepository(
    elasticsearchConverter: ElasticsearchConverter,
    objectMapper: ObjectMapper,
    esOperations: ReactiveElasticsearchOperations,
    elasticClient: ReactiveElasticsearchClient,
    esNameResolver: EsNameResolver
) : ElasticSearchRepository<EsTrait>(
    objectMapper,
    esOperations,
    esNameResolver.createEntityDefinitionExtended(EsTrait.ENTITY_DEFINITION),
    elasticsearchConverter,
    elasticClient,
    EsTrait::class.java,
    EsTrait::id.name,
) {

    override fun entityId(entity: EsTrait): String {
        return entity.id
    }

    suspend fun searchTraits(filter: EsTraitFilter): List<Trait> {
        val queryBuilder = QueryBuilders.boolQuery()
        val field = if (filter.listed) EsTrait::listedItemsCount.name else EsTrait::itemsCount.name
        queryBuilder.must(QueryBuilders.rangeQuery(field).gt(0))
        if (filter.blockchains.isNotEmpty()) {
            queryBuilder.must(QueryBuilders.termsQuery(EsTrait::blockchain.name, filter.blockchains))
        }
        if (filter.collectionIds.isNotEmpty()) {
            queryBuilder.must(QueryBuilders.termsQuery(EsTrait::collection.name, filter.collectionIds))
        }
        if (filter.keys.isNotEmpty()) {
            queryBuilder.must(QueryBuilders.termsQuery("${EsTrait::key.name}.raw", filter.keys))
        }
        if (filter.text?.isNotBlank() == true) {
            fullTextClauses(queryBuilder, filter.text!!, mapOf(EsTrait::key.name to 1.0f, EsTrait::value.name to 1.0f))
            queryBuilder.minimumShouldMatch(1)
        }

        val aggregationBuilder = AggregationBuilders.terms("keys")
            .field("${EsTrait::key.name}.raw")
            .size(filter.keysLimit)
            .subAggregation(
                AggregationBuilders.topHits("hits")
                    .fetchSource(true)
                    .size(filter.valuesLimit)
                    .sort(field, filter.valueFrequencySortOrder.toElasticsearchSortOrder())
            )
        val searchSourceBuilder = SearchSourceBuilder().aggregation(aggregationBuilder).size(0)
            .query(QueryBuilders.boolQuery().filter(queryBuilder))

        val searchRequest = SearchRequest().indices(entityDefinition.aliasName).source(searchSourceBuilder)

        val aggregation: Aggregation = elasticClient.aggregate(searchRequest).awaitFirstOrNull() ?: return emptyList()

        return (aggregation as ParsedStringTerms).toTraitsList(field)
    }

    private fun ParsedStringTerms.toTraitsList(field: String): List<Trait> =
        this.buckets
            .map { keysBucket ->
                val key = TraitEntry(value = keysBucket.keyAsString, count = keysBucket.docCount)
                val valuesBucket = keysBucket.aggregations.get<ParsedTopHits>("hits").hits
                key to valuesBucket
            }.map { (key, valuesBucket) ->
                val values = valuesBucket.map { value ->
                    val source = value.sourceAsMap
                    TraitEntry(
                        value = source["value"] as String,
                        count = (source[field] as Number).toLong()
                    )
                }
                Trait(
                    key = key.copy(count = values.sumOf { it.count }),
                    values = values
                )
            }
}
