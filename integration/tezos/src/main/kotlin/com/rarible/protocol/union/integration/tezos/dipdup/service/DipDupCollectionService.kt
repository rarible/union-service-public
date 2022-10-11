package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.dipdup.client.CollectionClient
import com.rarible.dipdup.client.exception.DipDupNotFound
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupCollectionConverter

class DipDupCollectionService(
    private val dipdupCollectionClient: CollectionClient,
) {

    suspend fun getCollectionsAll(continuation: String?, size: Int): Page<UnionCollection> {
        val page = dipdupCollectionClient.getCollectionsAll(
            limit = size,
            continuation = continuation,
            sortAsc = false
        )
        return Page(
            total = page.items.size.toLong(),
            continuation = page.continuation,
            entities = page.items.map { DipDupCollectionConverter.convert(it) }
        )
    }

    suspend fun getCollectionById(collectionId: String): UnionCollection {
        val collection = safeApiCall { dipdupCollectionClient.getCollectionById(collectionId) }
        return DipDupCollectionConverter.convert(collection)
    }

    suspend fun getCollectionByIds(ids: List<String>): List<UnionCollection> {
        val collections = dipdupCollectionClient.getCollectionsByIds(ids)
        return collections.map { DipDupCollectionConverter.convert(it) }
    }

    private suspend fun <T> safeApiCall(clientCall: suspend () -> T): T {
        return try {
            clientCall()
        } catch (e: DipDupNotFound) {
            throw UnionNotFoundException(message = e.message ?: "")
        }
    }
}
