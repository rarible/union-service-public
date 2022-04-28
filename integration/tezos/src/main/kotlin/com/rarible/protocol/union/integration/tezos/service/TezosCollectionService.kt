package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.tezos.api.client.NftCollectionControllerApi
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.tezos.converter.TezosCollectionConverter
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktCollectionService
import kotlinx.coroutines.reactive.awaitFirst

@CaptureSpan(type = "blockchain")
open class TezosCollectionService(
    private val collectionControllerApi: NftCollectionControllerApi,
    private val tzktCollectionService: TzktCollectionService
) : AbstractBlockchainService(BlockchainDto.TEZOS), CollectionService {

    override suspend fun getAllCollections(
        continuation: String?,
        size: Int
    ): Page<UnionCollection> {
        if (tzktCollectionService.enabled()) {
            return tzktCollectionService.getAllCollections(continuation, size)
        }
        val collections = collectionControllerApi.searchNftAllCollections(
            size,
            continuation
        ).awaitFirst()
        return TezosCollectionConverter.convert(collections, blockchain)
    }

    override suspend fun getCollectionById(collectionId: String): UnionCollection {
        if (tzktCollectionService.enabled()) {
            tzktCollectionService.getCollectionById(collectionId)
        }
        val collection = collectionControllerApi.getNftCollectionById(collectionId).awaitFirst()
        return TezosCollectionConverter.convert(collection, blockchain)
    }

    override suspend fun refreshCollectionMeta(collectionId: String) {
        // TODO[TEZOS]: implement.
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<UnionCollection> {
        val items = collectionControllerApi.searchNftCollectionsByOwner(
            owner,
            size,
            continuation
        ).awaitFirst()
        return TezosCollectionConverter.convert(items, blockchain)
    }
}
