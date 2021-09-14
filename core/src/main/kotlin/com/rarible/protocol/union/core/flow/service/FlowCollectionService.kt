package com.rarible.protocol.union.core.flow.service

import com.rarible.protocol.flow.nft.api.client.FlowNftCollectionControllerApi
import com.rarible.protocol.union.core.flow.converter.FlowUnionCollectionConverter
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.dto.FlowBlockchainDto
import com.rarible.protocol.union.dto.UnionCollectionDto
import com.rarible.protocol.union.dto.UnionCollectionsDto
import kotlinx.coroutines.reactive.awaitFirst

class FlowCollectionService(
    blockchain: FlowBlockchainDto,
    private val collectionControllerApi: FlowNftCollectionControllerApi
) : AbstractFlowService(blockchain), CollectionService {

    override suspend fun getAllCollections(continuation: String?, size: Int): UnionCollectionsDto {
        val collections = collectionControllerApi.searchNftAllCollections(continuation, size).awaitFirst()
        return FlowUnionCollectionConverter.convert(collections, blockchain)
    }

    override suspend fun getCollectionById(collectionId: String): UnionCollectionDto {
        val collection = collectionControllerApi.getNftCollectionById(collectionId).awaitFirst()
        return FlowUnionCollectionConverter.convert(collection, blockchain)
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): UnionCollectionsDto {
        val items = collectionControllerApi.searchNftCollectionsByOwner(owner, continuation, size).awaitFirst()
        return FlowUnionCollectionConverter.convert(items, blockchain)
    }
}