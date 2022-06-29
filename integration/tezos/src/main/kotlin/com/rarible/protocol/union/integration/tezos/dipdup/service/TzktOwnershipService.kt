package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.continuation.page.Page

interface TzktOwnershipService {

    fun enabled() = false

    suspend fun getOwnershipById(ownershipId: String): UnionOwnership {
        TODO("Not implemented")
    }

    suspend fun getOwnershipsByItem(
        itemId: String,
        continuation: String?,
        size: Int
    ): Page<UnionOwnership> {
        TODO("Not implemented")
    }

}
