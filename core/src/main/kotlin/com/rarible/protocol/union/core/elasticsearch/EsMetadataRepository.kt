package com.rarible.protocol.union.core.elasticsearch

import com.rarible.protocol.union.core.model.EsMetadata
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.stereotype.Component

@Component
class EsMetadataRepository(
    private val esOperations: ElasticsearchOperations,
    esNameResolver: EsNameResolver
) {
    val metadataIndexCoordinate = esNameResolver.metadataIndexCoordinate

    fun findById(id: String): EsMetadata? {
        return esOperations.get(id, EsMetadata::class.java, metadataIndexCoordinate)
    }

    fun save(metadata: EsMetadata): EsMetadata {
        return esOperations.save(metadata, metadataIndexCoordinate)
    }
}
