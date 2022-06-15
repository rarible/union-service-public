package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.dto.continuation.page.Page
import java.math.BigInteger

interface TzktCollectionService {

    fun enabled() = false

    suspend fun getAllCollections(
        continuation: String?,
        size: Int
    ): Page<UnionCollection> {
        TODO("Not implemented")
    }

    suspend fun getCollectionById(collectionId: String): UnionCollection {
        TODO("Not implemented")
    }

    suspend fun getCollectionByIds(collectionIds: List<String>): List<UnionCollection> {
        TODO("Not implemented")
    }

    suspend fun getCollectionByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<UnionCollection> {
        TODO("Not implemented")
    }

    suspend fun tokenCount(collectionId: String): BigInteger {
        TODO("Not implemented")
    }

}
