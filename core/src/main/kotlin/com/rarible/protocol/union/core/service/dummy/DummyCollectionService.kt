package com.rarible.protocol.union.core.service.dummy

import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.UnionCollectionTokenId
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page

class DummyCollectionService(
    blockchain: BlockchainDto
) : AbstractBlockchainService(blockchain), CollectionService {

    override suspend fun generateTokenId(
        collectionId: String,
        minter: String?
    ): UnionCollectionTokenId {
        throw UnionNotFoundException("Collection [$collectionId] not found, ${blockchain.name} is not available")
    }

    override suspend fun getAllCollections(
        continuation: String?,
        size: Int
    ): Page<UnionCollection> = Page.empty()

    override suspend fun getCollectionById(collectionId: String): UnionCollection {
        throw UnionNotFoundException("Collection [$collectionId] not found, ${blockchain.name} is not available")
    }

    override suspend fun refreshCollectionMeta(collectionId: String) {
        // Do nothing?
    }

    override suspend fun getCollectionMetaById(collectionId: String): UnionCollectionMeta {
        throw UnionNotFoundException("Meta for Collection [$collectionId] not found, ${blockchain.name} is not available")
    }

    override suspend fun getCollectionsByIds(ids: List<String>): List<UnionCollection> {
        return emptyList()
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<UnionCollection> {
        return Page.empty()
    }
}
