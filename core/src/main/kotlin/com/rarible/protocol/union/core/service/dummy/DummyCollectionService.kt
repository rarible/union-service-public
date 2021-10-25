package com.rarible.protocol.union.core.service.dummy

import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto

class DummyCollectionService(
    blockchain: BlockchainDto
) : AbstractBlockchainService(blockchain), CollectionService {

    override suspend fun getAllCollections(
        continuation: String?,
        size: Int
    )
            : Page<CollectionDto> {
        return Page.empty()
    }

    override suspend fun getCollectionById(collectionId: String): CollectionDto {
        throw UnionNotFoundException("Collection [$collectionId] not found, ${blockchain.name} is not available")
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<CollectionDto> {
        return Page.empty()
    }
}