package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.UnionCollectionTokenId
import com.rarible.protocol.union.core.service.router.BlockchainService
import com.rarible.protocol.union.dto.continuation.page.Page

interface CollectionService : BlockchainService {

    suspend fun generateTokenId(
        collectionId: String,
        minter: String?
    ): UnionCollectionTokenId

    suspend fun getAllCollections(
        continuation: String?,
        size: Int
    ): Page<UnionCollection>

    suspend fun getCollectionById(
        collectionId: String
    ): UnionCollection

    suspend fun getCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<UnionCollection>

    suspend fun refreshCollectionMeta(
        collectionId: String
    )

    suspend fun getCollectionMetaById(
        collectionId: String
    ): UnionCollectionMeta

    suspend fun getCollectionsByIds(
        ids: List<String>
    ): List<UnionCollection>
}
