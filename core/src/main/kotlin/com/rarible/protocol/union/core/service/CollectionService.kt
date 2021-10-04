package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.core.continuation.Page
import com.rarible.protocol.union.dto.CollectionDto

interface CollectionService : BlockchainService {

    suspend fun getAllCollections(
        continuation: String?,
        size: Int
    ): Page<CollectionDto>

    suspend fun getCollectionById(
        collectionId: String
    ): CollectionDto

    suspend fun getCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<CollectionDto>

}