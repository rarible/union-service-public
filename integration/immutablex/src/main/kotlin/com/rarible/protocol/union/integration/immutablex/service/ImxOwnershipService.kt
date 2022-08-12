package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.protocol.union.core.continuation.UnionOwnershipContinuation
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexAsset
import com.rarible.protocol.union.integration.immutablex.client.ImxActivityClient
import com.rarible.protocol.union.integration.immutablex.client.ImxAssetClient
import com.rarible.protocol.union.integration.immutablex.client.TokenIdDecoder
import com.rarible.protocol.union.integration.immutablex.converter.ImxOwnershipConverter
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class ImxOwnershipService(
    private val assetClient: ImxAssetClient,
    private val activityClient: ImxActivityClient
) : AbstractBlockchainService(BlockchainDto.IMMUTABLEX), OwnershipService {

    override suspend fun getOwnershipById(ownershipId: String): UnionOwnership {
        val itemId = TokenIdDecoder.decodeItemId(ownershipId.substringBeforeLast(":"))
        val owner = ownershipId.substringAfterLast(":")
        val asset = assetClient.getById(itemId)

        if (asset.user == owner) {
            val creator = activityClient.getItemCreator(itemId)
            return ImxOwnershipConverter.convert(asset, creator, blockchain)
        } else {
            throw UnionNotFoundException("Ownership ${blockchain}:${ownershipId} not found")
        }
    }

    override suspend fun getOwnershipsByIds(ownershipIds: List<String>): List<UnionOwnership> {
        val itemIds = ownershipIds.map { TokenIdDecoder.decodeItemId(it.substringBeforeLast(":")) }.toSet()

        val creators = coroutineScope { async { activityClient.getItemCreators(itemIds) } }
        val assets = assetClient.getByIds(itemIds)

        val ownershipIdsSet = HashSet(ownershipIds)
        val found = assets.filter {
            ownershipIdsSet.contains("${it.itemId}:${it.user}")
        }

        return ImxOwnershipConverter.convert(found, creators.await(), blockchain)
    }

    override suspend fun getOwnershipsAll(continuation: String?, size: Int): Slice<UnionOwnership> {
        val page = assetClient.getAllAssets(continuation, size, null, null, false)
        val converted = convert(page.result)
        return Paging(UnionOwnershipContinuation.ByLastUpdatedAndId, converted).getSlice(size)
    }

    override suspend fun getOwnershipsByItem(itemId: String, continuation: String?, size: Int): Page<UnionOwnership> {
        val decodedItemId = TokenIdDecoder.decodeItemId(itemId)
        val creator = coroutineScope { async { activityClient.getItemCreator(decodedItemId) } }
        val asset = assetClient.getById(decodedItemId)
        val result = ImxOwnershipConverter.convert(asset, creator.await(), blockchain)
        return Page(0L, null, listOf(result))
    }

    override suspend fun getOwnershipsByOwner(address: String, continuation: String?, size: Int): Page<UnionOwnership> {
        val assets = assetClient.getAssetsByOwner(address, continuation, size).result
        val result = convert(assets)
        return Paging(UnionOwnershipContinuation.ByLastUpdatedAndId, result).getPage(size, 0)
    }

    private suspend fun convert(assets: Collection<ImmutablexAsset>): List<UnionOwnership> {
        val creators = activityClient.getItemCreators(assets.map { it.itemId })
        return ImxOwnershipConverter.convert(assets, creators, blockchain)
    }
}
