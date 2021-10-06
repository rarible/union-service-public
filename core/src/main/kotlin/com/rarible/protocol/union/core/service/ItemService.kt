package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.core.continuation.Page
import com.rarible.protocol.union.core.service.router.BlockchainService
import com.rarible.protocol.union.dto.UnionItemDto

interface ItemService : BlockchainService {

    suspend fun getAllItems(
        continuation: String?,
        size: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): Page<UnionItemDto>

    suspend fun getItemById(
        itemId: String
    ): UnionItemDto

    suspend fun resetItemMeta(
        itemId: String
    )

    suspend fun getItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int
    ): Page<UnionItemDto>

    suspend fun getItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int
    ): Page<UnionItemDto>

    suspend fun getItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<UnionItemDto>

}