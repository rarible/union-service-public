package com.rarible.protocol.union.core.ethereum.service

import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.union.core.ethereum.converter.EthCollectionConverter
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionsDto
import kotlinx.coroutines.reactive.awaitFirst

class EthereumCollectionService(
    blockchain: BlockchainDto,
    private val collectionControllerApi: NftCollectionControllerApi
) : AbstractEthereumService(blockchain), CollectionService {

    override suspend fun getAllCollections(continuation: String?, size: Int): CollectionsDto {
        val collections = collectionControllerApi.searchNftAllCollections(continuation, size).awaitFirst()
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
    ): CollectionsDto {
        val items = collectionControllerApi.searchNftCollectionsByOwner(owner, continuation, size).awaitFirst()
        return EthCollectionConverter.convert(items, blockchain)
    }
}