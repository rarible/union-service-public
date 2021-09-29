package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemsDto

interface ItemService : BlockchainService {

    suspend fun getAllItems(
        continuation: String?,
        size: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): ItemsDto

    suspend fun getItemById(
        itemId: String
    ): ItemDto

    suspend fun getItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int
    ): ItemsDto

    suspend fun getItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int
    ): ItemsDto

    suspend fun getItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): ItemsDto

}