package com.rarible.protocol.union.integration.solana.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.solana.api.client.CollectionControllerApi
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.solana.converter.SolanaCollectionConverter
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull

@CaptureSpan(type = "blockchain")
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

    override suspend fun getCollectionsByOwner(owner: String, continuation: String?, size: Int): Page<UnionCollection> {
        val result = collectionApi.getCollectionsByOwner(owner, continuation, size).awaitFirst()
        return SolanaCollectionConverter.convert(result, blockchain)
    }

    override suspend fun refreshCollectionMeta(collectionId: String) {
        collectionApi.refreshCollectionMeta(collectionId).awaitFirstOrNull()
    }

    override suspend fun getCollectionsByIds(ids: List<String>): Page<UnionCollection> {
        // TODO: improve implementation.
        val collections = ids.map { getCollectionById(it) }
        return Page(
            total = collections.size.toLong(),
            continuation = null,
            entities = collections
        )
    }

}
