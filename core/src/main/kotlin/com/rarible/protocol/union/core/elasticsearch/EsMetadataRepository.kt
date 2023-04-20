package com.rarible.protocol.union.core.elasticsearch

import com.rarible.protocol.union.core.model.elastic.EsMetadata
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.stereotype.Component

@Component
class EsMetadataRepository(
    private val esOperations: ReactiveElasticsearchOperations,
    esNameResolver: EsNameResolver
) {
    val metadataIndexCoordinate = esNameResolver.metadataIndexCoordinate

    suspend fun findById(id: String): EsMetadata? {
        return esOperations.get(id, EsMetadata::class.java, metadataIndexCoordinate).awaitSingleOrNull()
    }

    suspend fun save(metadata: EsMetadata): EsMetadata {
        return esOperations.save(metadata, metadataIndexCoordinate).awaitSingle()
    }
}
