package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.protocol.union.core.continuation.UnionItemContinuation
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexActivityClient
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexAssetClient
import com.rarible.protocol.union.integration.immutablex.converter.ImmutablexItemConverter
import com.rarible.protocol.union.integration.immutablex.converter.ImmutablexItemMetaConverter
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexAsset
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class ImmutablexItemService(
    private val assetClient: ImmutablexAssetClient,
    private val activityClient: ImmutablexActivityClient
) : AbstractBlockchainService(BlockchainDto.IMMUTABLEX), ItemService {

    override suspend fun getAllItems(
        continuation: String?,
        size: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?,
    ): Page<UnionItem> {
        val page = assetClient.getAllAssets(continuation, size, lastUpdatedTo, lastUpdatedFrom)
        val result = convert(page.result)
        return Paging(UnionItemContinuation.ByLastUpdatedAndId, result).getPage(size, 0)
    }

    override suspend fun getItemById(itemId: String): UnionItem {
        val creatorDeferred = coroutineScope { async { activityClient.getItemCreator(itemId) } }
        val asset = assetClient.getById(itemId)
        return ImmutablexItemConverter.convert(asset, creatorDeferred.await(), blockchain)
    }

    override suspend fun getItemRoyaltiesById(itemId: String): List<RoyaltyDto> {
        val asset = assetClient.getById(itemId)
        return ImmutablexItemConverter.convertToRoyaltyDto(asset, blockchain)
    }

    override suspend fun getItemMetaById(itemId: String): UnionMeta {
        val asset = assetClient.getById(itemId)
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
        val page = assetClient.getAssetsByCollection(collection, owner, continuation, size)
        val converted = convert(page.result)
        return Paging(UnionItemContinuation.ByLastUpdatedAndId, converted).getPage(size, 0)
    }

    override suspend fun getItemsByCreator(creator: String, continuation: String?, size: Int): Page<UnionItem> {
        val mints = activityClient.getMints(
            size,
            continuation,
            user = creator,
            sort = ActivitySortDto.LATEST_FIRST
        ).result.associateBy { it.itemId() }

        // TODO what if some of items not found?
        val converted = getItemsByIds(mints.keys.toList())
            // TODO This is the only option to build working paging here ATM
            .map { it.copy(lastUpdatedAt = mints[it.id.value]!!.timestamp) }

        return Paging(UnionItemContinuation.ByLastUpdatedAndId, converted).getPage(size, 0)

    }

    override suspend fun getItemsByOwner(owner: String, continuation: String?, size: Int): Page<UnionItem> {
        val page = assetClient.getAssetsByOwner(owner, continuation, size)
        val converted = convert(page.result)
        return Paging(UnionItemContinuation.ByLastUpdatedAndId, converted).getPage(size, 0)
    }

    override suspend fun getItemsByIds(itemIds: List<String>): List<UnionItem> {
        val creators = coroutineScope { async { activityClient.getItemCreators(itemIds) } }
        val assets = assetClient.getByIds(itemIds)
        return convert(assets, creators.await())
    }

    override suspend fun getItemCollectionId(itemId: String): String {
        return itemId.substringBefore(":")
    }

    private suspend fun convert(assets: Collection<ImmutablexAsset>): List<UnionItem> {
        val creators = activityClient.getItemCreators(assets.map { it.itemId })
        return convert(assets, creators)
    }

    private fun convert(assets: Collection<ImmutablexAsset>, creators: Map<String, String>): List<UnionItem> {
        return ImmutablexItemConverter.convert(assets, creators, blockchain)
    }
}
