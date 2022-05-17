package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.tezos.dipdup.converter.TzktCollectionConverter
import com.rarible.tzkt.client.CollectionClient

class TzktCollectionServiceImpl(
    val collectionClient: CollectionClient
) : TzktCollectionService {

    private val blockchain = BlockchainDto.TEZOS

    override fun enabled() = true

    override suspend fun getAllCollections(
        continuation: String?,
        size: Int
    ): Page<UnionCollection> {
        val tzktPage = collectionClient.collections(size, continuation)
        return TzktCollectionConverter.convert(tzktPage, blockchain)
    }

    override suspend fun getCollectionById(collectionId: String): UnionCollection {
        val tzktCollection = collectionClient.collection(collectionId)
        return TzktCollectionConverter.convert(tzktCollection, blockchain)
    }

}
