package com.rarible.protocol.union.enrichment.repository.search

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.model.EsCollection
import com.rarible.protocol.union.core.model.EsCollectionFilter
import com.rarible.protocol.union.core.model.EsCollectionLite
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.enrichment.repository.search.internal.EsCollectionQueryBuilderService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.stereotype.Component
import java.io.IOException

@Component
@CaptureSpan(type = SpanType.DB)
class EsCollectionRepository(
    private val esOperations: ReactiveElasticsearchOperations,
    private val queryBuilderService: EsCollectionQueryBuilderService,
    esNameResolver: EsNameResolver
) : EsRepository {
    val entityDefinition = esNameResolver.createEntityDefinitionExtended(EsCollection.ENTITY_DEFINITION)
    private val clazz = EsCollection::class.java

    suspend fun saveAll(collections: List<EsCollection>): List<EsCollection> {
        return esOperations.saveAll(collections, entityDefinition.writeIndexCoordinates).collectList().awaitSingle()
    }

    suspend fun findById(collectionId: String): EsCollection? {
        return esOperations.get(collectionId, clazz, entityDefinition.searchIndexCoordinates).awaitFirstOrNull()
    }

    suspend fun search(filter: EsCollectionFilter, limit: Int?): List<EsCollectionLite> {
        val query = queryBuilderService.build(filter)
        query.maxResults = PageSize.COLLECTION.limit(limit)

        return search(query)
    }

    suspend fun search(query: NativeSearchQuery): List<EsCollectionLite> {
        return esOperations.search(query, EsCollectionLite::class.java, entityDefinition.searchIndexCoordinates)
            .collectList().awaitFirst().map { it.content }
    }

    override suspend fun refresh() {
        val refreshRequest = RefreshRequest().indices(entityDefinition.aliasName, entityDefinition.writeAliasName)

        try {
            esOperations.execute { it.indices().refreshIndex(refreshRequest) }.awaitFirstOrNull()
        } catch (e: IOException) {
            throw RuntimeException(entityDefinition.writeAliasName + " refreshModifyIndex failed", e)
        }
    }
}
