package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.integration.ethereum.EthereumComponent
import com.rarible.protocol.union.integration.ethereum.PolygonComponent
import com.rarible.protocol.union.integration.ethereum.converter.EthCollectionConverter
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

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

@Component
@EthereumComponent
class EthereumCollectionService(
    @Qualifier("ethereum.collection.api") collectionControllerApi: NftCollectionControllerApi
) : EthCollectionService(
    BlockchainDto.ETHEREUM,
    collectionControllerApi
)

@Component
@PolygonComponent
class PolygonCollectionService(
    @Qualifier("polygon.collection.api") collectionControllerApi: NftCollectionControllerApi
) : EthCollectionService(
    BlockchainDto.POLYGON,
    collectionControllerApi
)