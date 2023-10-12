package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionLazyItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.router.BlockchainService
import com.rarible.protocol.union.dto.LazyItemBurnFormDto
import com.rarible.protocol.union.dto.LazyItemMintFormDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.continuation.page.Page

interface ItemService : BlockchainService {

    suspend fun getAllItems(
        continuation: String?,
        size: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): Page<UnionItem>

    suspend fun getItemById(
        itemId: String
    ): UnionItem

    suspend fun getItemRoyaltiesById(
        itemId: String
    ): List<RoyaltyDto>

    suspend fun getItemMetaById(
        itemId: String
    ): UnionMeta

    suspend fun resetItemMeta(
        itemId: String
    )

    suspend fun getItemsByCollection(
        collection: String,
        owner: String?,
        continuation: String?,
        size: Int
    ): Page<UnionItem>

    suspend fun getItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int
    ): Page<UnionItem>

    suspend fun getItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<UnionItem>

    suspend fun getItemsByIds(
        itemIds: List<String>
    ): List<UnionItem>

    suspend fun getItemCollectionId(
        itemId: String
    ): String?

    suspend fun getLazyItemById(
        itemId: String
    ): UnionLazyItem

    suspend fun mintLazyItem(
        form: LazyItemMintFormDto
    ): UnionItem

    suspend fun burnLazyItem(
        form: LazyItemBurnFormDto
    )
}
