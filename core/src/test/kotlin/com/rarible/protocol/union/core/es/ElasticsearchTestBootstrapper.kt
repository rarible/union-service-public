package com.rarible.protocol.union.core.es

import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.elasticsearch.EsHelper.createAlias
import com.rarible.protocol.union.core.elasticsearch.EsHelper.createIndex
import com.rarible.protocol.union.core.elasticsearch.EsHelper.getIndexesByAlias
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.elasticsearch.EsRepository
import com.rarible.protocol.union.core.elasticsearch.bootstrap.metadataMappingIndex
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinition
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import java.util.concurrent.atomic.AtomicInteger

class ElasticsearchTestBootstrapper(
    private val esNameResolver: EsNameResolver,
    private val esOperations: ReactiveElasticsearchOperations,
    entityDefinitions: List<EntityDefinition>,
    private val repositories: List<EsRepository>,
) {
    private val metadataMapping = metadataMappingIndex()
    private val suffix = AtomicInteger(1)
    private val extendedEntityDefinitions: List<EntityDefinitionExtended> =
        entityDefinitions.map { esNameResolver.createEntityDefinitionExtended(it) }

    fun bootstrap() = runBlocking {
        logger.info("Initializing elasticsearch for test")
        createIndex(
            reactiveElasticSearchOperations = esOperations,
            name = esNameResolver.metadataIndexName,
            mapping = metadataMapping,
            settings = "{}"
        )
        for (entityDefinitionExtended in extendedEntityDefinitions) {
            logger.info("Create index for entity ${entityDefinitionExtended.entity}")
            refreshIndex(entityDefinitionExtended)

            logger.info("Finished elasticsearch initialization")
        }

        repositories.forEach { it.init() }
    }

    suspend fun removeAllIndexes(definition: EntityDefinitionExtended) {
        getIndexesByAlias(esOperations, definition.indexRootName).forEach {
            removeIndex(it)
        }
    }

    private suspend fun removeIndex(indexName: String) {
        logger.info("Removing index '$indexName'")
        esOperations.execute { it.indices().deleteIndex(DeleteIndexRequest(indexName)) }.awaitFirst()
    }

    private suspend fun refreshIndex(definition: EntityDefinitionExtended) {
        val newIndexName = definition.indexName(minorVersion = suffix.getAndIncrement())
        removeAllIndexes(definition)

        createIndex(
            reactiveElasticSearchOperations = esOperations,
            name = newIndexName,
            mapping = definition.mapping,
            settings = definition.settings
        )
        createAlias(
            reactiveElasticSearchOperations = esOperations, indexName = newIndexName, alias = definition.aliasName
        )
        createAlias(
            reactiveElasticSearchOperations = esOperations, indexName = newIndexName, alias = definition.writeAliasName
        )
    }

    companion object {
        private val logger by Logger()
    }
}
