package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.tezos.dipdup.converter.TzktCollectionConverter
import com.rarible.tzkt.client.CollectionClient
import com.rarible.tzkt.model.TzktNotFound

class TzktCollectionServiceImpl(
    val collectionClient: CollectionClient
) : TzktCollectionService {

    private val blockchain = BlockchainDto.TEZOS

    override fun enabled() = true

    override suspend fun getAllCollections(
        continuation: String?,
        size: Int
    ): Page<UnionCollection> {
        val tzktPage = collectionClient.collectionsAll(size, continuation)
        return TzktCollectionConverter.convert(tzktPage, blockchain)
    }

    override suspend fun getCollectionById(collectionId: String): UnionCollection {
        val tzktCollection = safeApiCall { collectionClient.collection(collectionId) }
        return TzktCollectionConverter.convert(tzktCollection, blockchain)
    }

    override suspend fun getCollectionByIds(collectionIds: List<String>): List<UnionCollection> {
        val tzktCollections = safeApiCall { collectionClient.collectionsByIds(collectionIds) }
        return TzktCollectionConverter.convert(tzktCollections, blockchain)
    }

    override suspend fun getCollectionByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<UnionCollection> {
        val tzktCollection = safeApiCall { collectionClient.collectionsByOwner(owner, size, continuation) }
        return TzktCollectionConverter.convert(tzktCollection, blockchain)
    }

    private suspend fun <T> safeApiCall(clientCall: suspend () -> T): T {
        return try {
            clientCall()
        } catch (e: TzktNotFound) {
            throw UnionNotFoundException(message = e.message ?: "")
        }
    }

}
