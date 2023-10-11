package com.rarible.protocol.union.integration.solana.service

import com.rarible.protocol.solana.api.client.CollectionControllerApi
import com.rarible.protocol.solana.dto.CollectionsByIdRequestDto
import com.rarible.protocol.union.core.exception.UnionException
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.UnionCollectionTokenId
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.solana.converter.SolanaCollectionConverter
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull

open class SolanaCollectionService(
    private val collectionApi: CollectionControllerApi
) : AbstractBlockchainService(BlockchainDto.SOLANA), CollectionService {

    override suspend fun getAllCollections(continuation: String?, size: Int): Page<UnionCollection> {
        val result = collectionApi.getAllCollections(continuation, size).awaitFirst()
        return SolanaCollectionConverter.convert(result, blockchain)
    }

    override suspend fun getCollectionById(collectionId: String): UnionCollection {
        val result = collectionApi.getCollectionById(collectionId).awaitFirst()
        return SolanaCollectionConverter.convert(result, blockchain)
    }

    override suspend fun getCollectionMetaById(collectionId: String): UnionCollectionMeta {
        // TODO[SOLANA]: implement in right way
        return getCollectionById(collectionId).meta
            ?: throw UnionNotFoundException("Meta not found for Collection $blockchain:$collectionId")
    }

    override suspend fun getCollectionsByOwner(owner: String, continuation: String?, size: Int): Page<UnionCollection> {
        val result = collectionApi.getCollectionsByOwner(owner, continuation, size).awaitFirst()
        return SolanaCollectionConverter.convert(result, blockchain)
    }

    override suspend fun refreshCollectionMeta(collectionId: String) {
        collectionApi.refreshCollectionMeta(collectionId).awaitFirstOrNull()
    }

    override suspend fun getCollectionsByIds(ids: List<String>): List<UnionCollection> {
        return collectionApi.searchCollectionsByIds(CollectionsByIdRequestDto(ids))
            .awaitFirst().collections.map {
                SolanaCollectionConverter.convert(it, blockchain)
            }
    }

    override suspend fun generateTokenId(collectionId: String, minter: String?): UnionCollectionTokenId {
        throw UnionException("Not supported by $blockchain")
    }
}
