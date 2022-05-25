package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexApiClient
import com.rarible.protocol.union.integration.immutablex.converter.ImmutablexCollectionConverter

class ImmutablexCollectionService(
    private val client: ImmutablexApiClient
): AbstractBlockchainService(BlockchainDto.IMMUTABLEX), CollectionService {

    override suspend fun getAllCollections(
        continuation: String?,
        size: Int
    ): Page<UnionCollection> {
        return ImmutablexCollectionConverter.convert(
            client.collectionsApi.getAll(continuation, size)
        )
    }

    override suspend fun getCollectionById(collectionId: String): UnionCollection {
        return ImmutablexCollectionConverter.convert(
            client.collectionsApi.getById(collectionId)
        )
    }

    override suspend fun getCollectionsByOwner(owner: String, continuation: String?, size: Int): Page<UnionCollection> {
        return Page.empty()
    }

    override suspend fun refreshCollectionMeta(collectionId: String) {
        return
    }

    override suspend fun getCollectionsByIds(ids: List<String>): List<UnionCollection> {
        TODO("Not yet implemented")
    }
}
