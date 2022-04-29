package com.rarible.protocol.union.core.elasticsearch

import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.elasticsearch.EsHelper.getRealName
import com.rarible.protocol.union.core.elasticsearch.EsHelper.moveAlias
import com.rarible.protocol.union.core.model.EsMetadata
import com.rarible.protocol.union.core.model.elasticsearch.CurrentEntityDefinition
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest
import org.elasticsearch.action.index.IndexRequest
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.stereotype.Service

@Service
class IndexService(
    private val reactiveElasticSearchOperations: ReactiveElasticsearchOperations,
    private val esMetadataRepository: EsMetadataRepository,
    esNameResolver: EsNameResolver
) {
    private val metadataIndexName: String = esNameResolver.metadataIndexName
    private val metadataIndexCoordinate: IndexCoordinates = IndexCoordinates.of(esNameResolver.metadataIndexName)

    suspend fun updateMetadata(entityDefinition: EntityDefinitionExtended) {
        innerUpdateMetadata(entityDefinition)
    }

    private suspend fun innerUpdateMetadata(entityDefinition: EntityDefinitionExtended) {
        val mappingId = EsEntityMetadataType.MAPPING.getId(entityDefinition)
        val mappingMetadata = esMetadataRepository.findById(mappingId)
        esMetadataRepository.save(
            mappingMetadata?.copy(content = entityDefinition.mapping)
                ?: EsMetadata(mappingId, entityDefinition.mapping)
        )
        val settingsId = EsEntityMetadataType.SETTINGS.getId(entityDefinition)
        val settingsMetadata = esMetadataRepository.findById(settingsId)
        esMetadataRepository.save(
            settingsMetadata?.copy(content = entityDefinition.settings)
                ?: EsMetadata(settingsId, entityDefinition.settings)
        )
        val versionId = EsEntityMetadataType.VERSION_DATA.getId(entityDefinition)
        val versionMetadata = esMetadataRepository.findById(versionId)
        esMetadataRepository.save(
            versionMetadata?.copy(content = entityDefinition.settings)
                ?: EsMetadata(versionId, entityDefinition.versionData.toString())
        )
        reactiveElasticSearchOperations.execute { it.indices().refreshIndex(RefreshRequest(metadataIndexName)) }
            .awaitFirstOrNull()
    }

    suspend fun getEntityMetadata(
        entity: EntityDefinitionExtended,
        realIndexName: String
    ): CurrentEntityDefinition? {

        val id = entity.name + EsEntityMetadataType.MAPPING.suffix
        var response = esMetadataRepository.findById(id)

        if (response == null) {
            logger.info("Index ${entity.name} exists with name $realIndexName but metadata does not. Update metadata")
            innerUpdateMetadata(entity)
            return null
        }
        val mapping = response.content

        response = esMetadataRepository.findById(entity.name + EsEntityMetadataType.SETTINGS.suffix)
        val settings: String = if (response != null) response.content else "{}"

        response = esMetadataRepository.findById(entity.name + EsEntityMetadataType.VERSION_DATA.suffix)
        val version: Int = if (response != null) response.content.toInt() else 1

        return CurrentEntityDefinition(
            mapping = mapping,
            settings = settings,
            versionData = version
        )
    }

    suspend fun finishIndexing(newIndexName: String, entityDefinition: EntityDefinitionExtended) {
        val realIndexName = getRealName(reactiveElasticSearchOperations, entityDefinition.aliasName)
            ?: throw IllegalStateException("Index not found")
        if (realIndexName != newIndexName) {
            moveAlias(reactiveElasticSearchOperations, entityDefinition.aliasName, realIndexName, newIndexName)
        }
        updateMetadata(entityDefinition)
    }

    companion object {
        private val logger by Logger()
    }
}
