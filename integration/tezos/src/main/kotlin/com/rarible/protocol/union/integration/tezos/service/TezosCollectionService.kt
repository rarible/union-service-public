package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.tezos.api.client.NftCollectionControllerApi
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.integration.tezos.converter.TezosCollectionConverter
import kotlinx.coroutines.reactive.awaitFirst

@CaptureSpan(type = "ext", subtype = "tezos")
open class TezosCollectionService(
    private val collectionControllerApi: NftCollectionControllerApi
) : AbstractBlockchainService(BlockchainDto.TEZOS), CollectionService {

    override suspend fun getAllCollections(
        continuation: String?,
        size: Int
    ): Page<CollectionDto> {
        val collections = collectionControllerApi.searchNftAllCollections(
            size,
            continuation
        ).awaitFirst()
        return TezosCollectionConverter.convert(collections, blockchain)
    }

    override suspend fun getCollectionById(collectionId: String): CollectionDto {
        val collection = collectionControllerApi.getNftCollectionById(collectionId).awaitFirst()
        return TezosCollectionConverter.convert(collection, blockchain)
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<CollectionDto> {
        val items = collectionControllerApi.searchNftCollectionsByOwner(
            owner,
            size,
            continuation
        ).awaitFirst()
        return TezosCollectionConverter.convert(items, blockchain)
    }
}