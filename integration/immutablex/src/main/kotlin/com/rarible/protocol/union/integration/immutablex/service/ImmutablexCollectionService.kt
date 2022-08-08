package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.protocol.union.core.continuation.UnionCollectionContinuation
import com.rarible.protocol.union.core.exception.UnionException
import com.rarible.protocol.union.core.model.TokenId
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexCollectionClient
import com.rarible.protocol.union.integration.immutablex.converter.ImmutablexCollectionConverter

class ImmutablexCollectionService(
    private val client: ImmutablexCollectionClient
) : AbstractBlockchainService(BlockchainDto.IMMUTABLEX), CollectionService {

    override suspend fun getAllCollections(
        continuation: String?,
        size: Int
    ): Page<UnionCollection> {
        val result = client.getAll(continuation, size).result
        val converted = result.map { ImmutablexCollectionConverter.convert(it, blockchain) }
        return Paging(UnionCollectionContinuation.ById, converted).getPage(size, 0)
    }

    override suspend fun getCollectionById(collectionId: String): UnionCollection {
        val result = client.getById(collectionId)
        return ImmutablexCollectionConverter.convert(result)
    }

    override suspend fun getCollectionsByIds(ids: List<String>): List<UnionCollection> {
        val result = client.getByIds(ids)
        return result.map { ImmutablexCollectionConverter.convert(it, blockchain) }
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<UnionCollection> {
        // TODO IMMUTABLEX does IMX collections have owner?
        return Page.empty()
    }

    override suspend fun refreshCollectionMeta(collectionId: String) {
        // Not applicable
    }

    override suspend fun generateNftTokenId(collectionId: String, minter: String?): TokenId {
        throw UnionException("Not supported by $blockchain")
    }
}
