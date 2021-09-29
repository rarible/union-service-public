package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionsDto

interface CollectionService : BlockchainService {

    suspend fun getAllCollections(
        continuation: String?,
        size: Int
    ): CollectionsDto

    suspend fun getCollectionById(
        collectionId: String
    ): CollectionDto

    suspend fun getCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): CollectionsDto

}