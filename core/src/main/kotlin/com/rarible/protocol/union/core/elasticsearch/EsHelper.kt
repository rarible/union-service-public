package com.rarible.protocol.union.core.elasticsearch

import org.elasticsearch.ElasticsearchStatusException
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.rest.RestStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object EsHelper {

    fun createIndex(client: RestHighLevelClient, name: String, settings: String, mapping: String) {
        try {
            client.indices().get(GetIndexRequest(name), RequestOptions.DEFAULT)
            logger.info("Index $name already exists")
        } catch (e: ElasticsearchStatusException) {
            logger.info("Creating index $name")
            if (e.status() == RestStatus.NOT_FOUND) {
                val request = CreateIndexRequest(name)
                    .settings(settings, XContentType.JSON)
                    .mapping(mapping, XContentType.JSON)
                client.indices().create(request, RequestOptions.DEFAULT)
            } else {
                throw e
            }
        }
    }

    fun moveAlias(client: RestHighLevelClient, alias: String, fromIndex: String, toIndex: String) {
        removeAlias(client, fromIndex, alias)
        createAlias(client, toIndex, alias)
    }

    private fun removeAlias(client: RestHighLevelClient, indexName: String, alias: String) {
        logger.info("Removing alias '$alias' from '$indexName'")
        val realName = getRealName(client, indexName)
        val existingAliases = getAliasesOfIndex(client, realName)
        if (alias !in existingAliases) {
            logger.info("Alias '$alias' does not exist in '$indexName' ('$realName')")
            return
        }
        val request = IndicesAliasesRequest()
            .addAliasAction(
                IndicesAliasesRequest.AliasActions
                    .remove()
                    .index(realName)
                    .alias(alias)
            )
        client.indices().updateAliases(request, RequestOptions.DEFAULT)
    }

    fun createAlias(client: RestHighLevelClient, indexName: String, alias: String) {
        logger.info("Adding alias '$alias' to '$indexName'")
        val realName = getRealName(client, indexName)
        val request = IndicesAliasesRequest()
            .addAliasAction(
                IndicesAliasesRequest.AliasActions
                    .add()
                    .index(realName)
                    .alias(alias)
            )
        client.indices().updateAliases(request, RequestOptions.DEFAULT)
    }

    fun getRealName(client: RestHighLevelClient, indexName: String): String {
        val response = client.indices().get(GetIndexRequest(indexName), RequestOptions.DEFAULT)
        return response.aliases.entries.first().key
    }

    private fun getAliasesOfIndex(client: RestHighLevelClient, indexName: String?): List<String> {
        val response = client.indices().get(GetIndexRequest(indexName), RequestOptions.DEFAULT)
        return response.aliases.entries.first().value.map { it.alias }
    }

   private val logger: Logger = LoggerFactory.getLogger(EsHelper::class.java)
}
