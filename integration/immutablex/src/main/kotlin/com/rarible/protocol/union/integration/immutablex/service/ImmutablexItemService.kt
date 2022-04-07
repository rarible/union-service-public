package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexApiClient
import com.rarible.protocol.union.integration.immutablex.converter.ImmutablexItemConverter
import com.rarible.protocol.union.integration.immutablex.converter.ImmutablexItemMetaConverter

class ImmutablexItemService(
    private val client: ImmutablexApiClient
): AbstractBlockchainService(BlockchainDto.IMMUTABLEX), ItemService {

    override suspend fun getAllItems(
        continuation: String?,
        size: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?,
    ): Page<UnionItem> {
        val page = client.getAllAssets(continuation, size, lastUpdatedTo, lastUpdatedFrom)
        return Page(
            total = page.result.size.toLong(),
            continuation = if (page.remaining) page.cursor else null,
            entities = page.result.map { ImmutablexItemConverter.convert(it, blockchain) }
        )
    }

    override suspend fun getItemById(itemId: String): UnionItem {
        val asset = client.getAsset(itemId)
        return ImmutablexItemConverter.convert(asset, blockchain)

    }

    override suspend fun getItemRoyaltiesById(itemId: String): List<RoyaltyDto> {
        val asset = client.getAsset(itemId)
        return ImmutablexItemConverter.convert(asset, blockchain).royalties
    }

    override suspend fun getItemMetaById(itemId: String): UnionMeta {
        val asset = client.getAsset(itemId)
        return ImmutablexItemMetaConverter.convert(asset)
    }

    override suspend fun resetItemMeta(itemId: String) {
        /** do nothing*/
    }

    override suspend fun getItemsByCollection(
        collection: String,
        owner: String?,
        continuation: String?,
        size: Int,
    ): Page<UnionItem> {
        val page = client.getAssetsByCollection(collection, owner, continuation, size)
        return Page(
            total = page.result.size.toLong(),
            continuation = if (page.remaining) page.cursor else null,
            entities = page.result.map { ImmutablexItemConverter.convert(it, blockchain) }
        )
    }

    override suspend fun getItemsByCreator(creator: String, continuation: String?, size: Int): Page<UnionItem> {
        val page = client.getAssetsByCreator(creator, continuation, size)
        return Page(
            total = page.result.size.toLong(),
            continuation = if (page.remaining) page.cursor else null,
            entities = page.result.map { ImmutablexItemConverter.convert(it, blockchain) }
        )
    }

    override suspend fun getItemsByOwner(owner: String, continuation: String?, size: Int): Page<UnionItem> {
        val page = client.getAssetsByOwner(owner, continuation, size)
        return Page(
            total = page.result.size.toLong(),
            continuation = if (page.remaining) page.cursor else null,
            entities = page.result.map { ImmutablexItemConverter.convert(it, blockchain) }
        )
    }

    override suspend fun getItemsByIds(itemIds: List<String>): List<UnionItem> {
        return client.getAssetsByIds(itemIds).map { ImmutablexItemConverter.convert(it, blockchain) }
    }
}
