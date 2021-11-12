package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.integration.ethereum.converter.EthCollectionConverter
import kotlinx.coroutines.reactive.awaitFirst

sealed class EthCollectionService(
    blockchain: BlockchainDto,
    private val collectionControllerApi: NftCollectionControllerApi
) : AbstractBlockchainService(blockchain), CollectionService {

    override suspend fun getAllCollections(
        continuation: String?,
        size: Int
    ): Page<CollectionDto> {
        val collections = collectionControllerApi.searchNftAllCollections(
            continuation,
            size
        ).awaitFirst()
        return EthCollectionConverter.convert(collections, blockchain)
    }

    override suspend fun getCollectionById(collectionId: String): CollectionDto {
        val collection = collectionControllerApi.getNftCollectionById(collectionId).awaitFirst()
        return EthCollectionConverter.convert(collection, blockchain)
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
        return EthCollectionConverter.convert(items, blockchain)
    }
}

@CaptureSpan(type = "network", subtype = "ethereum")
class EthereumCollectionService(
    collectionControllerApi: NftCollectionControllerApi
) : EthCollectionService(
    BlockchainDto.ETHEREUM,
    collectionControllerApi
)

@CaptureSpan(type = "network", subtype = "polygon")
class PolygonCollectionService(
    collectionControllerApi: NftCollectionControllerApi
) : EthCollectionService(
    BlockchainDto.POLYGON,
    collectionControllerApi
)