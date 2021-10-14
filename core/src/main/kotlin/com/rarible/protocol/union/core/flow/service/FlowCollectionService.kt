package com.rarible.protocol.union.core.flow.service

import com.rarible.protocol.flow.nft.api.client.FlowNftCollectionControllerApi
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.flow.converter.FlowCollectionConverter
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import kotlinx.coroutines.reactive.awaitFirst

class FlowCollectionService(
    blockchain: BlockchainDto,
    private val collectionControllerApi: FlowNftCollectionControllerApi
) : AbstractBlockchainService(blockchain), CollectionService {

    override suspend fun getAllCollections(continuation: String?, size: Int): Page<CollectionDto> {
        val collections = collectionControllerApi.searchNftAllCollections(continuation, size).awaitFirst()
        return FlowCollectionConverter.convert(collections, blockchain)
    }

    override suspend fun getCollectionById(collectionId: String): CollectionDto {
        val collection = collectionControllerApi.getNftCollectionById(collectionId).awaitFirst()
        return FlowCollectionConverter.convert(collection, blockchain)
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<CollectionDto> {
        val items = collectionControllerApi.searchNftCollectionsByOwner(owner, continuation, size).awaitFirst()
        return FlowCollectionConverter.convert(items, blockchain)
    }
}