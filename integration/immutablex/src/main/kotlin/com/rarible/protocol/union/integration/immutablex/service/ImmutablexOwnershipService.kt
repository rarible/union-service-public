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
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexActivityClient
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexAssetClient
import com.rarible.protocol.union.integration.immutablex.converter.ImmutablexOwnershipConverter
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexAsset
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class ImmutablexOwnershipService(
    private val assetClient: ImmutablexAssetClient,
    private val activityClient: ImmutablexActivityClient
) : AbstractBlockchainService(BlockchainDto.IMMUTABLEX), OwnershipService {

    override suspend fun getOwnershipById(ownershipId: String): UnionOwnership {
        val itemId = ownershipId.substringBeforeLast(":")
        val owner = ownershipId.substringAfterLast(":")
        val asset = assetClient.getById(itemId)

        if (asset.user == owner) {
            val creator = activityClient.getItemCreator(itemId)
            return ImmutablexOwnershipConverter.convert(asset, creator, blockchain)
        } else {
            throw UnionNotFoundException("Ownership ${blockchain}:${ownershipId} not found")
        }
    }

    override suspend fun getOwnershipsByIds(ownershipIds: List<String>): List<UnionOwnership> {
        val itemIds = ownershipIds.map { it.substringBeforeLast(":") }.toSet()

        val creators = coroutineScope { async { activityClient.getItemCreators(itemIds) } }
        val assets = assetClient.getByIds(itemIds)

        val ownershipIdsSet = HashSet(ownershipIds)
        val found = assets.filter {
            ownershipIdsSet.contains("${it.itemId}:${it.user}")
        }

        return ImmutablexOwnershipConverter.convert(found, creators.await(), blockchain)
    }

    override suspend fun getOwnershipsAll(continuation: String?, size: Int): Slice<UnionOwnership> {
        val page = assetClient.getAllAssets(continuation, size, null, null)
        val converted = convert(page.result)
        return Paging(UnionOwnershipContinuation.ByLastUpdatedAndId, converted).getSlice(size)
    }

    override suspend fun getOwnershipsByItem(itemId: String, continuation: String?, size: Int): Page<UnionOwnership> {
        val creator = coroutineScope { async { activityClient.getItemCreator(itemId) } }
        val asset = assetClient.getById(itemId)
        val result = ImmutablexOwnershipConverter.convert(asset, creator.await(), blockchain)
        return Page(0L, null, listOf(result))
    }

    override suspend fun getOwnershipsByOwner(address: String, continuation: String?, size: Int): Page<UnionOwnership> {
        val assets = assetClient.getAssetsByOwner(address, continuation, size).result
        val result = convert(assets)
        return Paging(UnionOwnershipContinuation.ByLastUpdatedAndId, result).getPage(size, 0)
    }

    private suspend fun convert(assets: Collection<ImmutablexAsset>): List<UnionOwnership> {
        val creators = activityClient.getItemCreators(assets.map { it.itemId })
        return ImmutablexOwnershipConverter.convert(assets, creators, blockchain)
    }
}
