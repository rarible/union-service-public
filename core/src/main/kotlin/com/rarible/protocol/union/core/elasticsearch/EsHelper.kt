package com.rarible.protocol.union.core.elasticsearch

import com.rarible.core.logging.Logger
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.common.xcontent.XContentType
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations

object EsHelper {

    suspend fun createIndex(
        reactiveElasticSearchOperations: ReactiveElasticsearchOperations,
        name: String,
        settings: String,
        mapping: String
    ) {
        val existIndex = reactiveElasticSearchOperations
            .execute { it.indices().existsIndex(GetIndexRequest(name)) }
            .awaitFirst()
        logger.info("Index $name exists = $existIndex")
        if (existIndex) return
        val request = CreateIndexRequest(name).settings(settings, XContentType.JSON).mapping(mapping, XContentType.JSON)
        reactiveElasticSearchOperations.execute { it.indices().createIndex(request) }.awaitFirstOrNull()
    }

    suspend fun moveAlias(
        reactiveElasticSearchOperations: ReactiveElasticsearchOperations,
        alias: String,
        fromIndex: String,
        toIndex: String
    ) {
        removeAlias(reactiveElasticSearchOperations, fromIndex, alias)
        createAlias(reactiveElasticSearchOperations, toIndex, alias, false)
    }

    suspend fun removeAlias(
        reactiveElasticSearchOperations: ReactiveElasticsearchOperations, indexName: String, alias: String
    ) {
        logger.info("Removing alias '$alias' from '$indexName'")
        val realName = getRealName(reactiveElasticSearchOperations, indexName)
        val existingAliases = getAliasesOfIndex(reactiveElasticSearchOperations, realName)
        if (alias !in existingAliases) {
            logger.info("Alias '$alias' does not exist in '$indexName' ('$realName')")
            return
        }
        val request = IndicesAliasesRequest().addAliasAction(
            IndicesAliasesRequest.AliasActions.remove().index(realName).alias(alias)
        )
        reactiveElasticSearchOperations.execute { it.indices().updateAliases(request) }.awaitFirstOrNull()
    }

    suspend fun createAlias(
        reactiveElasticSearchOperations: ReactiveElasticsearchOperations, indexName: String, alias: String,
        isWriteIndex: Boolean,
    ) {
        logger.info("Adding alias '$alias' to '$indexName'")
        val realName = getRealName(reactiveElasticSearchOperations, indexName)
        val request = IndicesAliasesRequest().addAliasAction(
            IndicesAliasesRequest.AliasActions.add().index(realName).alias(alias).writeIndex(false)
        )
        reactiveElasticSearchOperations.execute { it.indices().updateAliases(request) }.awaitFirstOrNull()
    }

    suspend fun getRealName(
        esOperations: ReactiveElasticsearchOperations, indexName: String
    ): String? {

        val exists = esOperations.execute { it.indices().existsIndex(GetIndexRequest(indexName)) }.awaitFirst()

        return if (exists) {
            val response = esOperations.execute { it.indices().getIndex(GetIndexRequest(indexName)) }.awaitFirst()
            response.aliases.entries.first().key
        } else null
    }

    suspend fun existsIndexesForEntity(
        esOperations: ReactiveElasticsearchOperations, indexPrefix: String
    ): Boolean = esOperations.execute { it.indices().existsIndex(GetIndexRequest("$indexPrefix*")) }.awaitFirst()

    suspend fun getIndexesByAlias(
        esOperations: ReactiveElasticsearchOperations, indexRootName: String
    ): List<String> {

        val exists = esOperations.execute { it.indices().existsIndex(GetIndexRequest("$indexRootName*")) }.awaitFirst()
        return if (exists) {
            val response =
                esOperations.execute { it.indices().getIndex(GetIndexRequest("$indexRootName*")) }.awaitFirst()
            response.aliases.entries.map { it.key }
        } else emptyList()
    }

    private suspend fun getAliasesOfIndex(
        reactiveElasticSearchOperations: ReactiveElasticsearchOperations, indexName: String?
    ): List<String> {
        val response =
            reactiveElasticSearchOperations.execute { it.indices().getIndex(GetIndexRequest(indexName)) }.awaitFirst()
        return response.aliases.entries.first().value.map { it.alias }
    }

    private val logger by Logger()
}
