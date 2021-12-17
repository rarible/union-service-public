package com.rarible.protocol.union.integration.flow.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.flow.nft.api.client.FlowNftCollectionControllerApi
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.flow.converter.FlowCollectionConverter
import kotlinx.coroutines.reactive.awaitFirst

@CaptureSpan(type = "blockchain")
open class FlowCollectionService(
    private val collectionControllerApi: FlowNftCollectionControllerApi
) : AbstractBlockchainService(BlockchainDto.FLOW), CollectionService {

    override suspend fun getAllCollections(
        continuation: String?,
        size: Int
    ): Page<CollectionDto> {
        val collections = collectionControllerApi.searchNftAllCollections(
            continuation,
            size
        ).awaitFirst()
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
        val items = collectionControllerApi.searchNftCollectionsByOwner(
            owner,
            continuation,
            size
        ).awaitFirst()
        return FlowCollectionConverter.convert(items, blockchain)
    }
}