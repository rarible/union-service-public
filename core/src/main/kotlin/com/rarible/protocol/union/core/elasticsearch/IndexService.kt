package com.rarible.protocol.union.core.elasticsearch

import com.rarible.protocol.union.core.elasticsearch.EsHelper.getRealName
import com.rarible.protocol.union.core.elasticsearch.EsHelper.moveAlias
import com.rarible.protocol.union.core.model.elastic.CurrentEntityDefinition
import com.rarible.protocol.union.core.model.elastic.EntityDefinitionExtended
import com.rarible.protocol.union.core.model.elastic.EsMetadata
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest
import org.slf4j.LoggerFactory
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.stereotype.Service

@Service
class IndexService(
    private val reactiveElasticSearchOperations: ReactiveElasticsearchOperations,
    private val esMetadataRepository: EsMetadataRepository,
    esNameResolver: EsNameResolver
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val metadataIndexName: String = esNameResolver.metadataIndexName

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
            versionMetadata?.copy(content = entityDefinition.versionData.toString())
                ?: EsMetadata(versionId, entityDefinition.versionData.toString())
        )

        reactiveElasticSearchOperations
            .execute { it.indices().refreshIndex(RefreshRequest(metadataIndexName)) }
            .awaitFirstOrNull()
    }

    suspend fun getEntityMetadata(
        definition: EntityDefinitionExtended,
        realIndexName: String
    ): CurrentEntityDefinition {
        val id = EsEntityMetadataType.MAPPING.getId(definition)
        var response: EsMetadata? = esMetadataRepository.findById(id)

        if (response == null) {
            logger.warn("Index ${definition.entity} exists with name $realIndexName but metadata does not. Update metadata")
            innerUpdateMetadata(definition)
            response = esMetadataRepository.findById(id)!!
        }

        val mapping = response.content

        response = esMetadataRepository.findById(EsEntityMetadataType.SETTINGS.getId(definition))!!
        val settings: String = response.content

        response = esMetadataRepository.findById(EsEntityMetadataType.VERSION_DATA.getId(definition))!!
        val version: Int = response.content.toInt()

        return CurrentEntityDefinition(
            mapping = mapping,
            settings = settings,
            versionData = version
        )
    }

    suspend fun finishIndexing(newIndexName: String, definition: EntityDefinitionExtended) {
        val alias = definition.aliasName
        val realIndexName = getRealName(reactiveElasticSearchOperations, alias, definition)
            ?: throw IllegalStateException("Index not found")
        if (realIndexName != newIndexName) {
            moveAlias(reactiveElasticSearchOperations, alias, realIndexName, newIndexName, definition)
        }
        updateMetadata(definition)
    }
}
