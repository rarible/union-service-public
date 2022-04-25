package com.rarible.protocol.union.core.elasticsearch

import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.stereotype.Service

@Service
class IndexMetadataService(
    private val client: RestHighLevelClient,
    private val esNameResolver: EsNameResolver
) {

    fun updateMetadata(entityDefinition: EntityDefinitionExtended, settings: String) {
        val mappingId = EsEntityMetadataType.MAPPING.getId(entityDefinition)
        client.index(
            IndexRequest(esNameResolver.metadataIndexName())
                .id(mappingId)
                .source(mapOf("id" to mappingId, "content" to entityDefinition.mapping)),
            RequestOptions.DEFAULT
        )
        val settingsId = EsEntityMetadataType.SETTINGS.getId(entityDefinition)
        client.index(
            IndexRequest(esNameResolver.metadataIndexName())
                .id(settingsId)
                .source(mapOf("id" to settingsId, "content" to settings)),
            RequestOptions.DEFAULT
        )
        val versionId = EsEntityMetadataType.VERSION_DATA.getId(entityDefinition)
        client.index(
            IndexRequest(esNameResolver.metadataIndexName())
                .id(versionId)
                .source(mapOf("id" to versionId, "content" to entityDefinition.versionData)),
            RequestOptions.DEFAULT
        )
        client.indices().refresh(RefreshRequest(esNameResolver.metadataIndexName()), RequestOptions.DEFAULT)
    }
}
