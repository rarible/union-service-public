package com.rarible.protocol.union.integration.tezos.service

import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupIntegrationProperties
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipDupItemService
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipDupRoyaltyService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemService

open class TezosItemService(
    private val tzktItemService: TzktItemService,
    private val dipDupItemService: DipDupItemService,
    private val dipDupRoyaltyService: DipDupRoyaltyService,
    private val properties: DipDupIntegrationProperties
) : AbstractBlockchainService(BlockchainDto.TEZOS), ItemService {

    override suspend fun getAllItems(
        continuation: String?,
        size: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): Page<UnionItem> {
        return if (properties.useDipDupTokens) {
            dipDupItemService.getAllItems(continuation, size)
        } else {
            tzktItemService.getAllItems(continuation, size, properties.tzktProperties.checkTokenBalance)
        }
    }

    override suspend fun getItemById(
        itemId: String
    ): UnionItem {
        return if (properties.useDipDupTokens) {
            dipDupItemService.getItemById(itemId)
        } else {
            tzktItemService.getItemById(itemId)
        }
    }

    override suspend fun getItemRoyaltiesById(itemId: String): List<RoyaltyDto> {
        return if (properties.useDipDupRoyalty) {
            var royalty = dipDupRoyaltyService.getItemRoyaltiesById(itemId)
            if (properties.saveDipDupRoyalty && royalty.isEmpty()) {
                royalty = tzktItemService.getItemRoyaltiesById(itemId)
                if (royalty.isNotEmpty()) {
                    dipDupRoyaltyService.saveRoyalty(itemId, royalty)
                }
            }
            royalty
        } else {
            tzktItemService.getItemRoyaltiesById(itemId)
        }
    }

    override suspend fun getItemMetaById(itemId: String): UnionMeta {
        return if (properties.useDipDupTokens) {
            dipDupItemService.getMetaById(itemId)
        } else {
            tzktItemService.getItemMetaById(itemId)
        }
    }

    override suspend fun resetItemMeta(itemId: String) {
        if (properties.useDipDupTokens) {
            dipDupItemService.resetMetaById(itemId)
        }
    }

    override suspend fun getItemsByCollection(
        collection: String,
        owner: String?,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        return if (properties.useDipDupTokens) {
            dipDupItemService.getItemsByCollection(collection, size, continuation)
        } else {
            tzktItemService.getItemsByCollection(collection, continuation, size)
        }
    }

    override suspend fun getItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        return if (properties.useDipDupTokens) {
            dipDupItemService.getItemsByCollection(creator, size, continuation)
        } else {
            tzktItemService.getItemsByCreator(creator, continuation, size)
        }
    }

    override suspend fun getItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        return if (properties.useDipDupTokens) {
            dipDupItemService.getItemsByOwner(owner, size, continuation)
        } else {
            tzktItemService.getItemsByOwner(owner, continuation, size)
        }
    }

    override suspend fun getItemsByIds(itemIds: List<String>): List<UnionItem> {
        if (itemIds.isEmpty()) return emptyList()
        return if (properties.useDipDupTokens) {
            dipDupItemService.getItemsByIds(itemIds)
        } else {
            tzktItemService.getItemsByIds(itemIds, properties.tzktProperties.checkTokenBalance)
        }
    }

    override suspend fun getItemCollectionId(itemId: String): String? {
        // TODO is validation possible here?
        return itemId.substringBefore(":")
    }
}
