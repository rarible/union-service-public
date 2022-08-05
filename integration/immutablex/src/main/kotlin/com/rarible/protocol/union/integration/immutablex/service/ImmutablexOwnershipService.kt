package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.core.common.mapAsync
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexActivityClient
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexAssetClient
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexAsset
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexAssetsPage
import java.math.BigInteger

class ImmutablexOwnershipService(
    private val assetClient: ImmutablexAssetClient,
    private val activityClient: ImmutablexActivityClient
) : AbstractBlockchainService(BlockchainDto.IMMUTABLEX), OwnershipService {

    override suspend fun getOwnershipById(ownershipId: String): UnionOwnership {
        val (contract, tokenId, owner) = IdParser.split(ownershipId, 3)
        return getAssetsByCollection(
            contract, owner, tokenId, null
        ) { contract, owner, cursor ->
            assetClient.getAssetsByCollection(contract, owner, cursor, 100)
        }?.let {
            convert(it)
        } ?: throw UnionNotFoundException("Ownership ${blockchain}:${ownershipId} not found")
    }

    override suspend fun getOwnershipsByIds(ownershipIds: List<String>): List<UnionOwnership> {
        return ownershipIds.chunked(8).map { chunk ->
            chunk.mapAsync {
                getOwnershipById(it)
            }
        }.flatten()
    }

    override suspend fun getOwnershipsAll(continuation: String?, size: Int): Slice<UnionOwnership> {
        TODO("Not yet implemented")
    }

    override suspend fun getOwnershipsByItem(itemId: String, continuation: String?, size: Int): Page<UnionOwnership> {
        val asset = assetClient.getById(itemId)
        return Page(0L, null, listOf(convert(asset)))
    }

    override suspend fun getOwnershipsByOwner(address: String, continuation: String?, size: Int): Page<UnionOwnership> {
        val assets = assetClient.getAssetsByOwner(address, continuation, size)
        return convert(assets)
    }

    private suspend fun getAssetsByCollection(
        contract: String,
        owner: String,
        tokenId: String,
        cursor: String?,
        fn: suspend (String, String, String?) -> ImmutablexAssetsPage
    ): ImmutablexAsset? {
        val page = fn(contract, owner, cursor)
        val asset = page.result.find { it.tokenId == tokenId }
        return if (asset == null && page.cursor.isNotEmpty()) {
            getAssetsByCollection(contract, owner, tokenId, page.cursor, fn)
        } else asset
    }

    private suspend fun convert(asset: ImmutablexAsset): UnionOwnership {
        val creator = activityClient.getItemCreator(asset.itemId)
        val creatorAddress = creator?.let { UnionAddressConverter.convert(blockchain, creator) }
        val ownerAddress = UnionAddressConverter.convert(blockchain, asset.user!!)
        return UnionOwnership(
            id = OwnershipIdDto(blockchain, asset.itemId, ownerAddress),
            collection = CollectionIdDto(blockchain, asset.tokenAddress),
            value = BigInteger.ONE,
            lazyValue = BigInteger.ZERO,
            createdAt = asset.createdAt!!,
            lastUpdatedAt = asset.updatedAt,
            creators = listOfNotNull(creatorAddress?.let { CreatorDto(creatorAddress, 1) })
        )
    }

    private suspend fun convert(page: ImmutablexAssetsPage): Page<UnionOwnership> {
        return Page(
            0L,
            page.cursor,
            page.result.map { convert(it) }
        )
    }
}
