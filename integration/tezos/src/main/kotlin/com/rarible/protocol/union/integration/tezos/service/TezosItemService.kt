package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemService

@CaptureSpan(type = "blockchain")
open class TezosItemService(
    private val tzktItemService: TzktItemService
) : AbstractBlockchainService(BlockchainDto.TEZOS), ItemService {

    override suspend fun getAllItems(
        continuation: String?,
        size: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): Page<UnionItem> {
        return tzktItemService.getAllItems(continuation, size)
    }

    override suspend fun getItemById(
        itemId: String
    ): UnionItem {
        return tzktItemService.getItemById(itemId)
    }

    override suspend fun getItemRoyaltiesById(itemId: String): List<RoyaltyDto> {
        return tzktItemService.getItemRoyaltiesById(itemId)
    }

    override suspend fun getItemMetaById(itemId: String): UnionMeta {
        return tzktItemService.getItemMetaById(itemId)
    }

    override suspend fun resetItemMeta(itemId: String) {
        // We can reset meta only for legacy backend
    }

    override suspend fun getItemsByCollection(
        collection: String,
        owner: String?,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        return tzktItemService.getItemsByCollection(collection, continuation, size)
    }

    override suspend fun getItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        return tzktItemService.getItemsByCreator(creator, continuation, size)
    }

    override suspend fun getItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        return tzktItemService.getItemsByOwner(owner, continuation, size)
    }

    override suspend fun getItemsByIds(itemIds: List<String>): List<UnionItem> {
        return tzktItemService.getItemsByIds(itemIds)
    }

    override suspend fun getItemCollectionId(itemId: String): String? {
        // TODO is validation possible here?
        return itemId.substringBefore(":")
    }
}
