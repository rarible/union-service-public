package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.dto.UnionCollectionDto
import com.rarible.protocol.union.dto.UnionCollectionsDto

interface CollectionService : BlockchainService {

    suspend fun getAllCollections(
        continuation: String?,
        size: Int
    ): UnionCollectionsDto

    suspend fun getCollectionById(
        collectionId: String
    ): UnionCollectionDto

    suspend fun getCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): UnionCollectionsDto

}