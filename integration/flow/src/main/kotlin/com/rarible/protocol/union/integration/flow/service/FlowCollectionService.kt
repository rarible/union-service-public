package com.rarible.protocol.union.integration.flow.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.flow.nft.api.client.FlowNftCollectionControllerApi
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.flow.converter.FlowCollectionConverter
import kotlinx.coroutines.reactor.awaitSingle

@CaptureSpan(type = "blockchain")
open class FlowCollectionService(
    private val collectionControllerApi: FlowNftCollectionControllerApi
) : AbstractBlockchainService(BlockchainDto.FLOW), CollectionService {

    override suspend fun getAllCollections(
        continuation: String?,
        size: Int
    ): Page<UnionCollection> {
        val collections = collectionControllerApi.searchNftAllCollections(
            continuation,
            size
        ).awaitSingle()
        return FlowCollectionConverter.convert(collections, blockchain)
    }

    override suspend fun getCollectionById(collectionId: String): UnionCollection {
        val collection = collectionControllerApi.getNftCollectionById(collectionId).awaitSingle()
        return FlowCollectionConverter.convert(collection, blockchain)
    }

    override suspend fun refreshCollectionMeta(collectionId: String) {
        // TODO[FLOW]: implement.
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<UnionCollection> {
        val items = collectionControllerApi.searchNftCollectionsByOwner(
            owner,
            continuation,
            size
        ).awaitSingle()
        return FlowCollectionConverter.convert(items, blockchain)
    }
}
