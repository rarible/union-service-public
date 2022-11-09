package com.rarible.protocol.union.api.service.api

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.dto.ItemsWithOwnershipDto
import com.rarible.protocol.union.dto.UnionAddress
import kotlinx.coroutines.flow.Flow

interface ItemQueryService {

    suspend fun getAllItems(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): ItemsDto

    suspend fun getAllItemIdsByCollection(collectionId: CollectionIdDto): Flow<ItemIdDto>
    suspend fun getItemsByIds(ids: List<ItemIdDto>): List<ItemDto>
    suspend fun getItemsByCollection(collection: CollectionIdDto, continuation: String?, size: Int?): ItemsDto
    suspend fun getItemsByCreator(
        creator: UnionAddress,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ItemsDto

    suspend fun getItemsByOwner(
        owner: UnionAddress,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ItemsDto

    suspend fun getItemsByOwnerWithOwnership(
        owner: UnionAddress,
        continuation: String?,
        size: Int?
    ): ItemsWithOwnershipDto
}
