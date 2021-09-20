package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.dto.UnionItemDto
import com.rarible.protocol.union.dto.UnionItemsDto

interface ItemService : BlockchainService {

    suspend fun getAllItems(
        continuation: String?,
        size: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): UnionItemsDto

    suspend fun getItemById(
        itemId: String
    ): UnionItemDto

    suspend fun getItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int
    ): UnionItemsDto

    suspend fun getItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int
    ): UnionItemsDto

    suspend fun getItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): UnionItemsDto

}