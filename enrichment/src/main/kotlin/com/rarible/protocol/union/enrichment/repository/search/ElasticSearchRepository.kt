package com.rarible.protocol.union.enrichment.repository.search

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.protocol.union.core.elasticsearch.EsHelper
import com.rarible.protocol.union.core.elasticsearch.EsRepository
import com.rarible.protocol.union.core.model.elastic.EntityDefinitionExtended
import com.rarible.protocol.union.core.model.elastic.EsSortOrder
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.repository.search.internal.mustMatchTerm
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.apache.lucene.queryparser.flexible.standard.QueryParserUtil
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.support.WriteRequest
import org.elasticsearch.client.Requests
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.MultiMatchQueryBuilder
import org.elasticsearch.index.query.Operator
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.xcontent.XContentType
import org.slf4j.LoggerFactory
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.ByQueryResponse
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.PostConstruct

abstract class ElasticSearchRepository<T>(
    val objectMapper: ObjectMapper,
    val esOperations: ReactiveElasticsearchOperations,
    val entityDefinition: EntityDefinitionExtended,
    private val elasticsearchConverter: ElasticsearchConverter,
    protected val elasticClient: ReactiveElasticsearchClient,
    private val entityType: Class<T>,
    private val idFieldName: String = "_id"
) : EsRepository {

    protected val logger = LoggerFactory.getLogger(javaClass)

    private val brokenEsState: AtomicBoolean = AtomicBoolean(true)

    abstract fun entityId(entity: T): String

    @PostConstruct
    override fun init() = runBlocking {
        val isBroken = try {
            val result = !EsHelper.existsIndexesForEntity(esOperations, entityDefinition.indexRootName)
            logger.info("Index ${entityDefinition.indexRootName} exists: $result")
            result
        } catch (e: Exception) {
            logger.error("Failed to get index state ${entityDefinition.indexRootName}: ${e.message}", e)
            true
        }
        brokenEsState.set(isBroken)
    }

    suspend fun findById(id: String): T? {
        return esOperations.get(id, entityType, entityDefinition.searchIndexCoordinates).awaitFirstOrNull()
    }

    // TODO used in tests only
    @Deprecated("Use bulk() instead")
    suspend fun save(entity: T): T {
        if (brokenEsState.get()) {
            throw IllegalStateException("No indexes to save")
        }

        return saveAll(listOf(entity))[0]
    }

    @Deprecated("Use bulk() instead")
    suspend fun saveAll(
        entities: List<T>,
        indexName: String? = null,
        refreshPolicy: WriteRequest.RefreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE
    ): List<T> {
        if (entities.isEmpty()) {
            logger.info("No entities to save")
            return emptyList()
        }

        if (brokenEsState.get()) {
            throw IllegalStateException("No indexes to save ($this)")
        }

        val bulkRequest = BulkRequest()
            .setRefreshPolicy(refreshPolicy)

        for (entity in entities) {
            val document = elasticsearchConverter.mapObject(entity)
            index(indexName).indexNames.forEach {
                bulkRequest.add(
                    Requests.indexRequest(it)
                        .id(entityId(entity))
                        .source(document, XContentType.JSON)
                        .create(false)
                )
            }
        }

        val result = elasticClient.bulk(bulkRequest).awaitFirst()

        if (result.hasFailures()) {
            logger.error(
                "Failed to saveAll [{}] entities to index [{}] with policy {}: {}",
                entities.size,
                index(indexName),
                refreshPolicy,
                result.buildFailureMessage()
            )
            throw RuntimeException(result.buildFailureMessage())
        }
        return entities
    }

    suspend fun bulk(
        entitiesToSave: List<T> = emptyList(),
        idsToDelete: List<String> = emptyList(),
        indexName: String? = null,
        refreshPolicy: WriteRequest.RefreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE,
    ) {
        if (entitiesToSave.isEmpty() && idsToDelete.isEmpty()) {
            logger.info("Nothing to save or delete")
            return
        }

        if (brokenEsState.get()) {
            throw IllegalStateException("No indexes to save")
        }

        val bulkRequest = BulkRequest()
            .setRefreshPolicy(refreshPolicy)

        for (entity in entitiesToSave) {
            val document = elasticsearchConverter.mapObject(entity)
            index(indexName).indexNames.forEach { index ->
                bulkRequest.add(
                    Requests.indexRequest(index)
                        .id(entityId(entity))
                        .source(document, XContentType.JSON)
                        .create(false)
                )
            }
        }

        for (id in idsToDelete) {
            index(indexName).indexNames.forEach { index ->
                bulkRequest.add(
                    Requests.deleteRequest(index)
                        .id(id)
                )
            }
        }

        val result = elasticClient.bulk(bulkRequest).awaitFirst()

        if (result.hasFailures()) {
            logger.error(
                "Failed to bulk entities to index [{}] with policy {}: {}",
                index(indexName),
                refreshPolicy,
                result.buildFailureMessage()
            )
            throw RuntimeException(result.buildFailureMessage())
        }
    }

    suspend fun deleteAll(ids: List<String>) {
        bulk(emptyList(), ids, refreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE)
        /*val query = CriteriaQuery(Criteria(idFieldName).`in`(ids))

        return esOperations.delete(
            query, entityType, entityDefinition.writeIndexCoordinates
        ).awaitFirstOrNull()?.deleted*/
    }

    override suspend fun refresh() {
        val refreshRequest = RefreshRequest().indices(entityDefinition.aliasName, entityDefinition.writeAliasName)

        try {
            esOperations.execute { it.indices().refreshIndex(refreshRequest) }.awaitFirstOrNull()
        } catch (e: IOException) {
            throw RuntimeException(entityDefinition.writeAliasName + " refreshModifyIndex failed", e)
        }
    }

    suspend fun deleteByBlockchain(blockchain: BlockchainDto): ByQueryResponse {
        val query = BoolQueryBuilder()
        query.mustMatchTerm(blockchain.name, "blockchain")
        val nativeQuery = NativeSearchQueryBuilder().withQuery(query).build()
        return esOperations.delete(nativeQuery, entityType, entityDefinition.searchIndexCoordinates).awaitSingle()
    }

    protected fun fullTextClauses(
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
        val joinedText = trimmedText.replace("\\s*".toRegex(), "")
        val joinedTextForSearch = "(${QueryParserUtil.escape(joinedText)} | ${QueryParserUtil.escape(joinedText)}*)"
        boolQuery.should(
            QueryBuilders.simpleQueryStringQuery(textForSearch)
                .defaultOperator(Operator.AND)
                .fuzzyTranspositions(false)
                .fuzzyMaxExpansions(0)
                .fields(fields)
        )
        boolQuery.should(
            QueryBuilders.simpleQueryStringQuery(joinedTextForSearch)
                .defaultOperator(Operator.AND)
                .fuzzyTranspositions(false)
                .fuzzyMaxExpansions(0)
                .fields(fields)
        )
        boolQuery.should(
            QueryBuilders.multiMatchQuery(text)
                .fields(fields)
                .fuzzyTranspositions(false)
                .operator(Operator.AND)
                .type(MultiMatchQueryBuilder.Type.PHRASE)
        )
    }

    protected suspend fun <T> logIfSlow(vararg params: Any?, query: suspend () -> T): T {
        val start = System.currentTimeMillis()
        val result = query()
        val latency = System.currentTimeMillis() - start
        if (latency >= MAX_SEARCH_LATENCY_MS) {
            logger.warn("Slow search: {} ms, params: {}", latency, params.filterNotNull().joinToString())
        }
        return result
    }

    protected fun EsSortOrder.toElasticsearchSortOrder(): SortOrder = when (this) {
        EsSortOrder.ASC -> SortOrder.ASC
        EsSortOrder.DESC -> SortOrder.DESC
    }

    private fun index(indexName: String?) = indexName
        ?.let { IndexCoordinates.of(it) } ?: entityDefinition.writeIndexCoordinates

    protected companion object {
        const val MAX_SEARCH_LATENCY_MS = 1500
    }
}
