package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.dto.continuation.page.Page

interface TzktCollectionService {

    fun enabled() = false

    suspend fun getAllCollections(
        continuation: String?,
        size: Int
    ): Page<UnionCollection> {
        throw UnionNotFoundException(null)
    }

    suspend fun getCollectionById(collectionId: String): UnionCollection {
        throw UnionNotFoundException(null)
    }

}
