package com.rarible.protocol.union.enrichment.repository

import com.mongodb.client.result.DeleteResult
import com.rarible.protocol.union.enrichment.model.ShortCollection
import com.rarible.protocol.union.enrichment.model.ShortCollectionId
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface CollectionRepository {
    suspend fun createIndices()
    suspend fun save(collection: ShortCollection): ShortCollection
    suspend fun get(collectionId: ShortCollectionId): ShortCollection?
    suspend fun delete(collectionId: ShortCollectionId): DeleteResult?
    suspend fun getAll(ids: List<ShortCollectionId>): List<ShortCollection>
    fun findWithMultiCurrency(lastUpdateAt: Instant): Flow<ShortCollection>
}
