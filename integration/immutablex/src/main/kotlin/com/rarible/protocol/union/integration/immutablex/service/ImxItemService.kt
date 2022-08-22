package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.protocol.union.core.continuation.UnionItemContinuation
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexAsset
import com.rarible.protocol.union.integration.immutablex.client.ImxActivityClient
import com.rarible.protocol.union.integration.immutablex.client.ImxAssetClient
import com.rarible.protocol.union.integration.immutablex.client.ImxCollectionClient
import com.rarible.protocol.union.integration.immutablex.client.TokenIdDecoder
import com.rarible.protocol.union.integration.immutablex.converter.ImxItemConverter
import com.rarible.protocol.union.integration.immutablex.converter.ImxItemMetaConverter
import com.rarible.protocol.union.integration.immutablex.model.ImxCollectionCreator
import com.rarible.protocol.union.integration.immutablex.repository.ImxCollectionCreatorRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class ImxItemService(
    private val assetClient: ImxAssetClient,
    private val activityClient: ImxActivityClient,
    private val collectionClient: ImxCollectionClient,
    private val collectionCreatorRepository: ImxCollectionCreatorRepository
) : AbstractBlockchainService(BlockchainDto.IMMUTABLEX), ItemService {

    override suspend fun getAllItems(
        continuation: String?,
        size: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?,
    ): Page<UnionItem> {
        val safeShowDeleted = showDeleted ?: false
        val page = assetClient.getAllAssets(continuation, size, lastUpdatedTo, lastUpdatedFrom, false, safeShowDeleted)
        val result = convert(page.result)
        return Paging(UnionItemContinuation.ByLastUpdatedAndId, result).getPage(size, 0)
    }

    override suspend fun getItemById(itemId: String): UnionItem {
        val decodedItemId = TokenIdDecoder.decodeItemId(itemId)
        val creatorDeferred = coroutineScope { async { getItemCreator(decodedItemId) } }
        val asset = assetClient.getById(decodedItemId)
        return ImxItemConverter.convert(asset, creatorDeferred.await(), blockchain)
    }

    override suspend fun getItemRoyaltiesById(itemId: String): List<RoyaltyDto> {
        val decodedItemId = TokenIdDecoder.decodeItemId(itemId)
        val asset = assetClient.getById(decodedItemId)
        return ImxItemConverter.convertToRoyaltyDto(asset, blockchain)
    }

    override suspend fun getItemMetaById(itemId: String): UnionMeta {
        val decodedItemId = TokenIdDecoder.decodeItemId(itemId)
        val asset = assetClient.getById(decodedItemId)
        return ImxItemMetaConverter.convert(asset, blockchain)
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
        // TODO doesn't work in right way, creator can't be defined by mint
        // The only hope we won't need this request since ES will be used for such requests
        val to = DateIdContinuation.parse(continuation)?.date
        val mints = activityClient.getMints(
            pageSize = size,
            continuation = null,
            to = to,
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
        val decodedIds = itemIds.map { TokenIdDecoder.decodeItemId(it) }
        val creators = coroutineScope { async { getItemCreators(decodedIds) } }
        val assets = assetClient.getByIds(decodedIds)
        return convert(assets, creators.await())
    }

    override suspend fun getItemCollectionId(itemId: String): String {
        return itemId.substringBefore(":")
    }

    suspend fun getItemCreator(itemId: String): String? {
        return getItemCreators(listOf(itemId)).values.firstOrNull()
    }

    suspend fun getItemCreators(assetIds: Collection<String>): Map<String, String> {
        if (assetIds.isEmpty()) {
            return emptyMap()
        }
        val mappedToCollection = assetIds.associateBy({ it }, { it.substringBefore(":") })
        val collectionIds = mappedToCollection.values.toSet()
        val fromCache = collectionCreatorRepository.getAll(collectionIds)
        val missing = collectionIds - (fromCache.map { it.collection }.toSet())
        val fromApi = collectionClient.getByIds(missing)
            .filter { !it.projectOwnerAddress.isNullOrBlank() }
            .map { ImxCollectionCreator(it.address, it.projectOwnerAddress!!) }

        val collections = (fromCache + fromApi).associateBy { it.collection }

        val result = HashMap<String, String>(assetIds.size)
        mappedToCollection.forEach { (itemId, collectionId) ->
            val creator = collections[collectionId]?.creator
            creator?.let { result[itemId] = it }
        }
        collectionCreatorRepository.saveAll(fromApi)
        return result
    }

    private suspend fun convert(assets: Collection<ImmutablexAsset>): List<UnionItem> {
        val creators = getItemCreators(assets.map { it.itemId })
        return convert(assets, creators)
    }

    private fun convert(assets: Collection<ImmutablexAsset>, creators: Map<String, String>): List<UnionItem> {
        return assets.map { ImxItemConverter.convert(it, creators[it.itemId], blockchain) }
    }
}
