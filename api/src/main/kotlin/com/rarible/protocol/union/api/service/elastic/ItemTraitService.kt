package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.core.EsProperties
import com.rarible.protocol.union.core.model.elastic.EsItem
import com.rarible.protocol.union.core.model.trait.ExtendedTraitProperty
import com.rarible.protocol.union.core.model.trait.Trait
import com.rarible.protocol.union.core.model.trait.TraitEntry
import com.rarible.protocol.union.core.model.trait.TraitProperty
import com.rarible.protocol.union.dto.TraitDto
import com.rarible.protocol.union.dto.TraitEntryDto
import com.rarible.protocol.union.dto.TraitsDto
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.common.unit.Fuzziness
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.MultiMatchQueryBuilder
import org.elasticsearch.index.query.Operator
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.aggregations.Aggregation
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.BucketOrder
import org.elasticsearch.search.aggregations.bucket.filter.FiltersAggregator
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilters
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class ItemTraitService(
    val elasticClient: ReactiveElasticsearchClient,
    val esItemRepository: EsItemRepository,
    val esProperties: EsProperties
) {

    companion object {
        private const val KEYS_BUCKET_NAME = "trait_keys"
        private const val VALUES_BUCKET_NAME = "trait_values"
        const val TRAIT_KEY_KEYWORD_FIELD = "traits.key.raw"
        const val TRAIT_VALUE_KEYWORD_FIELD = "traits.value.raw"
        val TRAITS_FIELDS = mapOf("traits.key" to 1f, "traits.value" to 1f)
    }

    suspend fun searchTraits(filter: String, collectionIds: List<String>): TraitsDto {
        val aggregationBuilder = getFilterAggregationBuilder(filter)

        val searchSourceBuilder = SearchSourceBuilder().aggregation(aggregationBuilder)
            .size(0)
            .apply {
                val boolQuery = QueryBuilders.boolQuery()

                if (collectionIds.isNotEmpty()) {
                    boolQuery.must(
                        QueryBuilders.boolQuery()
                            .must(QueryBuilders.termsQuery(EsItem::collection.name, collectionIds))
                    )
                }

                boolQuery.must(
                    QueryBuilders.boolQuery()
                        .apply {
                            nestedFullTextClauses(this, filter, TRAITS_NESTED_FIELDS)
                        }
                )
                query(boolQuery)
            }

        val searchRequest =
            SearchRequest().indices(esItemRepository.entityDefinition.aliasName).source(searchSourceBuilder)
        val aggregation: Aggregation = elasticClient.aggregate(searchRequest).awaitFirstOrNull() ?: return TraitsDto()

        return (aggregation as ParsedNested).aggregations.get<ParsedFilter>("traits").aggregations
            .toTraitList()
    }

    suspend fun getTraitsWithRarity(
        collectionId: String,
        properties: Set<TraitProperty>
    ): List<ExtendedTraitProperty> {
        val itemsCount = esItemRepository.countItemsInCollection(collectionId)
        if (itemsCount == 0L) return properties.map { it.toExtended() }

        val traits = getTraitsDistinct(collectionId, properties)

        return traits.map { trait ->
            trait.values.map { value ->
                value.toExtendedTraitProperty(key = trait.key.value, itemsCount = itemsCount.toBigDecimal())
            }
        }.flatten()
    }

    private fun TraitEntry.toExtendedTraitProperty(key: String, itemsCount: BigDecimal): ExtendedTraitProperty =
        ExtendedTraitProperty(
            key = key,
            value = value,
            rarity = count.toBigDecimal().multiply(100.toBigDecimal()).divide(itemsCount, 7, RoundingMode.HALF_UP)
        )

    suspend fun getTraitsDistinct(collectionId: String, properties: Set<TraitProperty>): List<Trait> {
        val collectionQueryBuilder = QueryBuilders.boolQuery()
            .must(QueryBuilders.termsQuery(EsItem::collection.name, collectionId))
        val query = QueryBuilders.boolQuery().filter(collectionQueryBuilder)

        val keyValueDelimiter = "\\0"
        val traitsFilters = properties.map {
            val queryBuilder = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery(TRAIT_KEY_KEYWORD_FIELD, it.key))
                .must(QueryBuilders.termQuery(TRAIT_VALUE_KEYWORD_FIELD, it.value))
            FiltersAggregator.KeyedFilter(
                "${it.key}$keyValueDelimiter${it.value}",
                QueryBuilders.nestedQuery(TRAIT_NESTED_FIELD, queryBuilder, ScoreMode.None)
            )
        }
        val filters = AggregationBuilders.filters("traits", *traitsFilters.toTypedArray())

        val searchSourceBuilder = SearchSourceBuilder().query(query).aggregation(filters).size(0)

        val searchRequest =
            SearchRequest().indices(esItemRepository.entityDefinition.aliasName).source(searchSourceBuilder)

        val aggregation: Aggregation = elasticClient.aggregate(searchRequest).awaitFirstOrNull() ?: return emptyList()

        return (aggregation as ParsedFilters).buckets
            .map {
                val (key, value) = it.keyAsString.split(keyValueDelimiter)
                key to TraitEntry(value = value, count = it.docCount)
            }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
            .map { (key, values) ->
                Trait(
                    key = TraitEntry(value = key, count = values.count { it.count > 0 }.toLong()),
                    values = values
                )
            }
    }

    private fun TraitProperty.toExtended() = ExtendedTraitProperty(key = key, value = value, rarity = BigDecimal.ZERO)

    private fun getFilterAggregationBuilder(
        filter: String
    ): NestedAggregationBuilder {
        val traitTermsAggregationBuilder = getTermsAggregationBuilder()
        return AggregationBuilders.nested("traits", TRAIT_NESTED_FIELD)
            .subAggregation(
                AggregationBuilders.filter(
                    "traits",
                    QueryBuilders.boolQuery()
                        .apply {
                            if (!filter.isNullOrEmpty()) {
                                fullTextClauses(this, filter, TRAITS_FIELDS)
                            }
                        }
                )
                    .subAggregation(traitTermsAggregationBuilder)
            )
    }

    private fun getTermsAggregationBuilder(): TermsAggregationBuilder {
        return AggregationBuilders.terms(KEYS_BUCKET_NAME)
            .field(TRAIT_KEY_KEYWORD_FIELD)
            .size(esProperties.itemsTraitsKeysLimit)
            .subAggregation(
                AggregationBuilders.terms(VALUES_BUCKET_NAME)
                    .field(TRAIT_VALUE_KEYWORD_FIELD)
                    .size(esProperties.itemsTraitsValuesLimit)
                    .order(BucketOrder.count(false))
            )
    }

    private fun fullTextClauses(
        boolQuery: BoolQueryBuilder,
        text: String,
        fields: Map<String, Float>
    ) {
        if (text.isBlank()) return
        val trimmedText = text.trim()
        val lastTerm = trimmedText.split(" ").last()
        val textForSearch = if (lastTerm == trimmedText) {
            "($lastTerm | $lastTerm*)"
        } else {
            trimmedText.replaceAfterLast(" ", "($lastTerm | $lastTerm*)")
        }
        boolQuery.should(
            QueryBuilders.simpleQueryStringQuery(textForSearch)
                .defaultOperator(Operator.AND)
                .fuzzyTranspositions(false)
                .fuzzyMaxExpansions(0)
                .fields(fields)
        )
            // phrase. boost = 100
            .should(
                QueryBuilders.multiMatchQuery(text)
                    .fields(fields)
                    .boost(100f)
                    .fuzzyTranspositions(false)
                    .operator(Operator.AND)
                    .type(MultiMatchQueryBuilder.Type.PHRASE)
            )
    }

    protected fun nestedFullTextClauses(
        boolQuery: BoolQueryBuilder,
        text: String,
        fields: Map<String, Map<String, Float>>
    ) {
        fields.forEach {
            boolQuery.should(
                QueryBuilders.nestedQuery(
                    it.key,
                    QueryBuilders.simpleQueryStringQuery(text)
                        .fields(it.value),
                    ScoreMode.Total
                )
            )
                .should(
                    QueryBuilders.nestedQuery(
                        it.key,
                        QueryBuilders.multiMatchQuery(text)
                            .boost(100f)
                            .fields(it.value),
                        ScoreMode.Total
                    )
                )
                .should(
                    QueryBuilders.nestedQuery(
                        it.key,
                        QueryBuilders.simpleQueryStringQuery(
                            if (text.isEmpty())
                                text
                            else
                                "${text.replace(" ", " AND ")}*"
                        )
                            .boost(0.01f)
                            .fields(it.value),
                        ScoreMode.Total
                    )
                )
                .should(
                    QueryBuilders.nestedQuery(
                        it.key,
                        QueryBuilders.multiMatchQuery(text)
                            .boost(0.0001f)
                            .type(MultiMatchQueryBuilder.Type.MOST_FIELDS)
                            .fuzziness(Fuzziness.AUTO)
                            .fields(it.value),
                        ScoreMode.Total
                    )
                )
        }
    }

    protected fun Aggregations.toTraitList(): TraitsDto =
        TraitsDto(
            traits = this.get<ParsedStringTerms>(KEYS_BUCKET_NAME).buckets
                .map { keysBucket ->
                    val key = TraitEntryDto(value = keysBucket.keyAsString, count = keysBucket.docCount)
                    val valuesBucket = keysBucket.aggregations.get<ParsedStringTerms>(VALUES_BUCKET_NAME).buckets
                    key to valuesBucket
                }.map { (key, valuesBucket) ->
                    TraitDto(
                        key = key,
                        values = valuesBucket.map { value ->
                            TraitEntryDto(
                                value = value.keyAsString,
                                count = value.docCount
                            )
                        }
                    )
                }
        )

    suspend fun queryTraits(collectionIds: List<String>, keys: List<String>?): TraitsDto {
        val traitTermsAggregationBuilder =
            getTermsAggregationBuilder().apply {
                if (keys?.isNotEmpty() == true) {
                    includeExclude(IncludeExclude(keys.toTypedArray(), null))
                }
            }
        val aggregationBuilder = AggregationBuilders.nested("traits", TRAIT_NESTED_FIELD)
            .subAggregation(traitTermsAggregationBuilder)
        val searchSourceBuilder = SearchSourceBuilder().aggregation(aggregationBuilder).size(0)
            .apply {
                if (collectionIds.isNotEmpty()) {
                    val queryBuilder = QueryBuilders.boolQuery()
                        .must(QueryBuilders.termsQuery(EsItem::collection.name, collectionIds))
                    query(QueryBuilders.boolQuery().filter(queryBuilder))
                }
            }
        val searchRequest = SearchRequest().indices(esItemRepository.entityDefinition.aliasName)
            .source(searchSourceBuilder)

        val aggregation: Aggregation = elasticClient.aggregate(searchRequest).awaitFirstOrNull() ?: return TraitsDto()

        return (aggregation as ParsedNested).aggregations.toTraitList()
    }
}
