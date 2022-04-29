package com.rarible.protocol.union.core.elasticsearch

import com.rarible.protocol.union.core.model.EsMetadata
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.stereotype.Component

@Component
class EsMetadataRepository(
    private val esOperations: ReactiveElasticsearchOperations,
    esNameResolver: EsNameResolver
) {
    val matadataIndexCoordinate = esNameResolver.matadataIndexCoordinate
    suspend fun findById(id: String): EsMetadata? {
        return esOperations.get(id, EsMetadata::class.java, matadataIndexCoordinate).awaitFirstOrNull()
    }

    suspend fun save(metadata: EsMetadata): EsMetadata {
        return esOperations.save(metadata, matadataIndexCoordinate).awaitFirst()
    }
}
