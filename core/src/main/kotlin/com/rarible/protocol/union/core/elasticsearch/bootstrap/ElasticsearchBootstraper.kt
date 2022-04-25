package com.rarible.protocol.union.core.elasticsearch.bootstrap

import com.rarible.protocol.union.core.elasticsearch.EsEntityMetadataType
import com.rarible.protocol.union.core.elasticsearch.EsHelper.createAlias
import com.rarible.protocol.union.core.elasticsearch.EsHelper.createIndex
import com.rarible.protocol.union.core.elasticsearch.EsHelper.getRealName
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver.Companion.METADATA_INDEX
import com.rarible.protocol.union.core.elasticsearch.ReindexSchedulingService
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinition
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended
import org.elasticsearch.ElasticsearchStatusException
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.client.indices.PutMappingRequest
import org.elasticsearch.common.xcontent.XContentType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ElasticsearchBootstraper(
    private val esNameResolver: EsNameResolver,
    private val client: RestHighLevelClient,
    entityDefinitions: List<EntityDefinition>,
    private val reindexSchedulingService: ReindexSchedulingService,
    private val forceUpdate: Set<String> = emptySet()
) {
    private val settings = indexSettings()
    private val metadataMapping = metadataIndex()
    private val extendedEntityDefinitions: List<EntityDefinitionExtended> =
        entityDefinitions.map { esNameResolver.createEntityDefinitionExtended(it) }

    fun bootstrap() {

        logger.info("Initializing elasticsearch")
        val metadataIndexName = esNameResolver.metadataIndexName()
        createIndex(
            client = client,
            name = metadataIndexName,
            mapping = metadataMapping,
            settings = "{}"
        )
        for (entity in extendedEntityDefinitions) {

            logger.info("Updating index for entity ${entity.name}")

            if (checkReindexInProgress(entity.writeAliasName)) continue

            try {
                updateIndexMapping(entity.aliasName, entity)
            } catch (e: ElasticsearchStatusException) {
                // Index does not exist
                createNewIndex(entity)
            }
        }
        logger.info("Finished elasticsearch initialization")
    }

    private fun checkReindexInProgress(writeAlias: String): Boolean {
        try {
            val currentWriteIndices =
                client.indices().get(GetIndexRequest(writeAlias), RequestOptions.DEFAULT).aliases.keys
            if (currentWriteIndices.size > 1) {
                logger.info("Reindex already in progress for index $writeAlias skipping update")
                return true
            }
        } catch (_: ElasticsearchStatusException) {
        }
        return false
    }

    private fun updateIndexMapping(
        alias: String,
        entity: EntityDefinitionExtended
    ) {
        val realIndexName = getRealName(client, alias)
        val currentIndexMappingResponse =
            getEntityMetadata(entity, realIndexName, EsEntityMetadataType.MAPPING) ?: return
        val currentIndexSettingsResponse =
            getEntityMetadata(entity, realIndexName, EsEntityMetadataType.SETTINGS) ?: return
        val currentVersionDataResponse =
            getEntityMetadata(entity, realIndexName, EsEntityMetadataType.VERSION_DATA) ?: return

        if (!currentIndexMappingResponse.isExists) {
            logger.info("Index ${entity.name} exists with name $realIndexName but metadata does not. Skipping index update")
            return
        }

        val currentIndexMapping = currentIndexMappingResponse.source["content"]
        val currentSettings = currentIndexSettingsResponse.source["content"]
        val currentVersionData = currentVersionDataResponse.source["content"]
        when {
            currentVersionData != entity.versionData -> recreateIndex(realIndexName, entity)
            currentIndexMapping != entity.mapping || forceUpdate.contains(entity.name) ->
                updateMappings(
                    realIndexName,
                    entity
                )
            currentSettings != settings -> updateSettings(realIndexName, entity)
            else -> logger.info("Index ${entity.name} mapping and settings has not changed. Skipping index update")
        }
    }

    private fun getEntityMetadata(
        entity: EntityDefinitionExtended,
        realIndexName: String,
        esEntityMetadataType: EsEntityMetadataType
    ): GetResponse? {
        val response = client.get(
            GetRequest(esNameResolver.metadataIndexName(), entity.name + esEntityMetadataType.suffix),
            RequestOptions.DEFAULT
        )
        if (!response.isExists) {
            logger.info("Index ${entity.name} exists with name $realIndexName but metadata does not. Skipping index update")
            return null
        }
        return response
    }

    private fun createNewIndex(entity: EntityDefinitionExtended) {
        val newIndexName = entity.indexName(minorVersion = 1)
        createIndex(
            client = client,
            name = newIndexName,
            mapping = entity.mapping,
            settings = settings
        )
        createAlias(
            client = client,
            indexName = newIndexName,
            alias = entity.aliasName
        )
        createAlias(
            client = client,
            indexName = newIndexName,
            alias = entity.writeAliasName
        )
        scheduleReindex(
            entity = entity,
            newIndexName = newIndexName
        )
    }

    private fun updateMappings(
        realIndexName: String,
        entity: EntityDefinitionExtended
    ) {
        try {
            logger.info("Index ${entity.name} mappings changed. Updating")
            client.indices().putMapping(
                PutMappingRequest(realIndexName)
                    .source(entity.mapping, XContentType.JSON),
                RequestOptions.DEFAULT
            )
            scheduleReindex(
                entity = entity,
                newIndexName = realIndexName,
            )
        } catch (e: ElasticsearchStatusException) {
            logger.info("Failed to update index ${entity.name} mapping. Recreating index")
            recreateIndex(realIndexName, entity)
        }
    }

    private fun updateSettings(
        realIndexName: String,
        entity: EntityDefinitionExtended
    ) {
        try {
            logger.info("Index ${entity.name} settings changed. Updating")
            client.indices().putSettings(
                UpdateSettingsRequest(realIndexName)
                    .settings(settings, XContentType.JSON),
                RequestOptions.DEFAULT
            )
        } catch (e: ElasticsearchStatusException) {
            logger.info("Failed to update index ${entity.name} settings. Recreating index")
            recreateIndex(realIndexName, entity)
        }
    }

    private fun recreateIndex(
        realIndexName: String,
        entity: EntityDefinitionExtended,
    ) {
        val indexVersion = entity.getVersion(realIndexName)
        val newIndexName = entity.indexName(minorVersion = indexVersion + 1)
        createIndex(
            client = client,
            name = newIndexName,
            mapping = entity.mapping,
            settings = settings
        )
        createAlias(
            client = client,
            indexName = newIndexName,
            alias = entity.writeAliasName
        )
        scheduleReindex(entity, newIndexName)
    }

    private fun scheduleReindex(
        entity: EntityDefinitionExtended,
        newIndexName: String
    ) {
        logger.info("Scheduling reindex for ${entity.name} to $newIndexName")
        reindexSchedulingService.scheduleReindex(newIndexName, entity, indexSettings())
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ElasticsearchBootstraper::class.java)
    }
}

fun indexSettings(): String {
    return ElasticsearchBootstraper::class.java.getResource("/mappings/settings.json")!!.readText()
}

fun metadataIndex(): String {
    return ElasticsearchBootstraper::class.java.getResource("/mappings/${METADATA_INDEX}.json")!!
        .readText()
}
