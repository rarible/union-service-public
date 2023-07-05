package com.rarible.protocol.union.core.es

import com.rarible.core.common.mapAsync
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.elasticsearch.EsHelper.createAlias
import com.rarible.protocol.union.core.elasticsearch.EsHelper.createIndex
import com.rarible.protocol.union.core.elasticsearch.EsHelper.existsIndex
import com.rarible.protocol.union.core.elasticsearch.EsHelper.getIndexesByAlias
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.elasticsearch.EsRepository
import com.rarible.protocol.union.core.elasticsearch.bootstrap.metadataMappingIndex
import com.rarible.protocol.union.core.model.elastic.EntityDefinition
import com.rarible.protocol.union.core.model.elastic.EntityDefinitionExtended
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest
import org.elasticsearch.index.query.MatchAllQueryBuilder
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import java.util.concurrent.atomic.AtomicInteger

class ElasticsearchTestBootstrapper(
    private val esNameResolver: EsNameResolver,
    private val esOperations: ReactiveElasticsearchOperations,
    private val restHighLevelClient: ReactiveElasticsearchClient,
    entityDefinitions: List<EntityDefinition>,
    private val repositories: List<EsRepository>,
) {
    private val metadataMapping = metadataMappingIndex()
    private val suffix = 1
    private val extendedEntityDefinitions: List<EntityDefinitionExtended> =
        entityDefinitions.map { esNameResolver.createEntityDefinitionExtended(it) }

    fun bootstrap(): Unit = runBlocking<Unit> {
        logger.info("Initializing elasticsearch for test")
        if (!existsIndex(esOperations, esNameResolver.metadataIndexName)) {
            createIndex(
                reactiveElasticSearchOperations = esOperations,
                name = esNameResolver.metadataIndexName,
                mapping = metadataMapping,
                settings = "{}"
            )
        }
        for (entityDefinitionExtended in extendedEntityDefinitions) {
            logger.info("Create index for entity ${entityDefinitionExtended.entity}")
            val newIndexName = entityDefinitionExtended.indexName(minorVersion = suffix)
            if (!existsIndex(esOperations, newIndexName)) {
                createIndex(
                    reactiveElasticSearchOperations = esOperations,
                    name = newIndexName,
                    mapping = entityDefinitionExtended.mapping,
                    settings = entityDefinitionExtended.settings
                )
            }
            createAlias(
                reactiveElasticSearchOperations = esOperations, indexName = newIndexName, alias = entityDefinitionExtended.aliasName,
            )
            createAlias(
                reactiveElasticSearchOperations = esOperations, indexName = newIndexName, alias = entityDefinitionExtended.writeAliasName,
            )
            logger.info("Finished elasticsearch initialization")
        }
        deleteDataInAllIndex(esNameResolver.metadataIndexName)
        repositories.forEach { it.init() }
    }

    private suspend fun createIndex(definition: EntityDefinitionExtended) {
    }

    suspend fun deleteDataInAllIndex(metadataIndexName: String) = coroutineScope {
        val indexesByAlias = getIndexesByAlias(esOperations, "")
        indexesByAlias
            .filter { it != metadataIndexName }
            .mapAsync { index ->
                restHighLevelClient.deleteBy { request ->
                    request
                        .setRefresh(true)
                        .setQuery(MatchAllQueryBuilder())
                        .indices(index)
                }
                    .doOnNext {
                        logger.info("deleted all data for index '$index'")
                    }.awaitSingle()

                esOperations.execute {
                    it.indices().refreshIndex(
                        RefreshRequest()
                            .indices(index)
                    )
                }.awaitFirstOrNull()
            }
    }

    suspend fun deleteAllIndexes() = coroutineScope {
        val indexesByAlias = getIndexesByAlias(esOperations, "")
        indexesByAlias
            .mapAsync { index ->
                esOperations.execute {
                    it.indices().deleteIndex { request ->
                        request.indices(index)
                    }
                }.awaitFirst()
            }
    }

    companion object {
        private val logger by Logger()
    }
}
