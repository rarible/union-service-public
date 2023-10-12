package com.rarible.protocol.union.core.service.dummy

import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionLazyItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.LazyItemBurnFormDto
import com.rarible.protocol.union.dto.LazyItemMintFormDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.continuation.page.Page

class DummyItemService(
    blockchain: BlockchainDto
) : AbstractBlockchainService(blockchain), ItemService {

    override suspend fun getAllItems(
        continuation: String?,
        size: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): Page<UnionItem> {
        return Page.empty()
    }

    override suspend fun getItemById(itemId: String): UnionItem {
        throw UnionNotFoundException("Item [$itemId] not found, ${blockchain.name} is not available")
    }

    override suspend fun getItemRoyaltiesById(itemId: String): List<RoyaltyDto> {
        throw UnionNotFoundException("Royalties for Item [$itemId] not found, ${blockchain.name} is not available")
    }

    override suspend fun getItemMetaById(itemId: String): UnionMeta {
        throw UnionNotFoundException("Meta for Item [$itemId] not found, ${blockchain.name} is not available")
    }

    override suspend fun resetItemMeta(itemId: String) {
    }

    override suspend fun getItemsByCollection(
        collection: String,
        owner: String?,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        return Page.empty()
    }

    override suspend fun getItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        return Page.empty()
    }

    override suspend fun getItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<UnionItem> {
        return Page.empty()
    }

    override suspend fun getItemsByIds(itemIds: List<String>): List<UnionItem> {
        return emptyList()
    }

    override suspend fun getItemCollectionId(itemId: String): String? {
        return null
    }

    override suspend fun getLazyItemById(itemId: String): UnionLazyItem {
        throw UnionNotFoundException("Item [$itemId] not found, ${blockchain.name} is not available")
    }

    override suspend fun mintLazyItem(form: LazyItemMintFormDto): UnionItem {
        throw UnionNotFoundException("Lazy Mint is prohibited, ${blockchain.name} is not available")
    }

    override suspend fun burnLazyItem(form: LazyItemBurnFormDto) {
        throw UnionNotFoundException("Burn is prohibited, ${blockchain.name} is not available")
    }
}
