package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.dipdup.client.OwnershipClient
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupOwnershipConverter

class DipDupOwnershipService(
    private val dipdupOwnershipClient: OwnershipClient,
) : DipDupService {

    suspend fun getOwnershipsAll(continuation: String?, size: Int): Slice<UnionOwnership> {
        val page = dipdupOwnershipClient.getOwnershipsAll(
            limit = size,
            continuation = continuation,
            sortAsc = false
        )
        return Slice(
            continuation = page.continuation,
            entities = page.items.map { DipDupOwnershipConverter.convert(it) }
        )
    }

    suspend fun getOwnershipById(ownershipId: String): UnionOwnership {
        val ownership = safeApiCall("Ownership $ownershipId wasn't found") { dipdupOwnershipClient.getOwnershipById(ownershipId) }
        return DipDupOwnershipConverter.convert(ownership)
    }

    suspend fun getOwnershipsByItem(
        itemId: String,
        continuation: String?,
        size: Int
    ): Page<UnionOwnership> {
        val page = dipdupOwnershipClient.getOwnershipsByItem(
            limit = size,
            itemId = itemId,
            continuation = continuation
        )
        return Page(
            total = page.items.size.toLong(),
            continuation = page.continuation,
            entities = page.items.map { DipDupOwnershipConverter.convert(it) }
        )
    }

    suspend fun getOwnershipsByIds(ids: List<String>): List<UnionOwnership> {
        val ownerships = dipdupOwnershipClient.getOwnershipsByIds(ids)
        return ownerships.map { DipDupOwnershipConverter.convert(it) }
    }
}
