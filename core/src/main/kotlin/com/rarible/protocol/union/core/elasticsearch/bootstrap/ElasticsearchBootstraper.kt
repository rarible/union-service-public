package com.rarible.protocol.union.core.elasticsearch.bootstrap

import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.elasticsearch.EsHelper.createAlias
import com.rarible.protocol.union.core.elasticsearch.EsHelper.createIndex
import com.rarible.protocol.union.core.elasticsearch.EsHelper.getRealName
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver.Companion.METADATA_INDEX
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.core.elasticsearch.ReindexSchedulingService
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinition
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.client.indices.PutMappingRequest
import org.elasticsearch.common.xcontent.XContentType
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations

class ElasticsearchBootstraper(
    private val esNameResolver: EsNameResolver,
    private val esOperations: ReactiveElasticsearchOperations,
    entityDefinitions: List<EntityDefinition>,
    private val reindexSchedulingService: ReindexSchedulingService,
    private val indexService: IndexService,
    private val forceUpdate: Set<String> = emptySet(),
) {

    private val metadataMapping = metadataIndex()

    private val extendedEntityDefinitions: List<EntityDefinitionExtended> =
        entityDefinitions.map { esNameResolver.createEntityDefinitionExtended(it) }

    fun bootstrap() = runBlocking {

        logger.info("Initializing elasticsearch")
        createIndex(
            reactiveElasticSearchOperations = esOperations,
            name = esNameResolver.metadataIndexName,
            mapping = metadataMapping,
            settings = "{}"
        )
        for (entity in extendedEntityDefinitions) {

            logger.info("Updating index for entity ${entity.name}")

            if (checkReindexInProgress(entity.writeAliasName)) {
                logger.info("Updating index for entity ${entity.name} is in progress. Skip")
                continue
            }
            updateIndexMapping(entity)
        }
        logger.info("Finished elasticsearch initialization")
    }

    private suspend fun checkReindexInProgress(writeAlias: String): Boolean {
        val exists = esOperations.execute { it.indices().existsIndex(GetIndexRequest(writeAlias)) }.awaitFirst()

        if (!exists) return false

        val currentWriteIndices =
            esOperations.execute { it.indices().getIndex(GetIndexRequest(writeAlias)) }
                .awaitFirst()
                .aliases.keys
        if (currentWriteIndices.size > 1) {
            logger.info("Reindex already in progress for index $writeAlias skipping update")
            return true
        }
        return false
    }

    private suspend fun updateIndexMapping(
        entity: EntityDefinitionExtended
    ) {
        val realIndexName = getRealName(esOperations, entity.aliasName)

        if (realIndexName == null) {
            createFirstIndex(entity)
            return
        }
        val currentEntityMetadata = indexService.getEntityMetadata(entity, realIndexName) ?: return
        when {
            currentEntityMetadata.versionData != entity.versionData -> recreateIndex(realIndexName, entity)
            currentEntityMetadata.settings != entity.settings -> recreateIndex(realIndexName, entity)
            currentEntityMetadata.mapping != entity.mapping || forceUpdate.contains(entity.name) ->
                updateMappings(
                    realIndexName,
                    entity
                )
            else -> logger.info("Index ${entity.name} mapping and settings has not changed. Skipping index update")
        }
    }

    private suspend fun createFirstIndex(entity: EntityDefinitionExtended) {
        val newIndexName = entity.indexName(minorVersion = entity.versionData)
        createIndex(
            reactiveElasticSearchOperations = esOperations,
            name = newIndexName,
            mapping = entity.mapping,
            settings = entity.settings
        )
        createAlias(
            reactiveElasticSearchOperations = esOperations,
            indexName = newIndexName,
            alias = entity.aliasName
        )
        createAlias(
            reactiveElasticSearchOperations = esOperations,
            indexName = newIndexName,
            alias = entity.writeAliasName
        )
        scheduleReindex(
            entity = entity,
            newIndexName = newIndexName
        )
    }

    private suspend fun updateMappings(
        realIndexName: String,
        entity: EntityDefinitionExtended
    ) {
        logger.info("Index ${entity.name} mappings changed. Updating")
        esOperations.execute {
            it.indices().putMapping(
                PutMappingRequest(realIndexName)
                    .source(entity.mapping, XContentType.JSON)
            )
        }
            .asFlow()
            .catch {
                logger.info("Failed to update index ${entity.name} mapping. Recreating index")
                recreateIndex(realIndexName, entity)
            }
            .onCompletion {
                scheduleReindex(
                    entity = entity,
                    newIndexName = realIndexName,
                )
            }
            .toList()
    }

    private suspend fun recreateIndex(
        realIndexName: String,
        entity: EntityDefinitionExtended,
    ) {
        val indexVersion = entity.getVersion(realIndexName)
        val newIndexName = entity.indexName(minorVersion = indexVersion + 1)
        createIndex(
            reactiveElasticSearchOperations = esOperations,
            name = newIndexName,
            mapping = entity.mapping,
            settings = entity.settings
        )
        createAlias(
            reactiveElasticSearchOperations = esOperations,
            indexName = newIndexName,
            alias = entity.writeAliasName
        )
        scheduleReindex(entity, newIndexName)
    }

    private fun scheduleReindex(
        entity: EntityDefinitionExtended,
        newIndexName: String
    ) = runBlocking {
        logger.info("Scheduling reindex for ${entity.name} to $newIndexName")
        reindexSchedulingService.scheduleReindex(newIndexName, entity)
    }

    companion object {
        private val logger by Logger()
    }
}

fun metadataIndex(): String {
    return ElasticsearchBootstraper::class.java.getResource("/mappings/${METADATA_INDEX}.json")!!
        .readText()
}
