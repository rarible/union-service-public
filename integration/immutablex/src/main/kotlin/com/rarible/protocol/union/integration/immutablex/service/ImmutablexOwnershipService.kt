package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.group
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexApiClient
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexAsset
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexAssetsPage
import java.math.BigInteger

class ImmutablexOwnershipService(
    private val client: ImmutablexApiClient
) : AbstractBlockchainService(BlockchainDto.IMMUTABLEX), OwnershipService {

    override suspend fun getOwnershipById(ownershipId: String): UnionOwnership {
        val (contract, tokenId, owner) = ownershipId.split(":")
        return getAssetsByCollection(
            contract, owner, tokenId.toBigInteger(), null
        ) { contract, owner, cursor ->
            client.getAssetsByCollection(contract, owner, cursor, 100)
        }?.let {
            convert(it)
        } ?: throw UnionNotFoundException("Ownership ${blockchain}:${ownershipId} not found")
    }

    override suspend fun getOwnershipsByItem(itemId: String, continuation: String?, size: Int): Page<UnionOwnership> {
        val asset = client.getAsset(itemId)
        return Page(0L, null, listOf(convert(asset)))
    }

    override suspend fun getOwnershipsByOwner(address: String, continuation: String?, size: Int): Page<UnionOwnership> {
        return convert(
            client.getAssetsByOwner(address, continuation, size)
        )
    }

    private suspend fun getAssetsByCollection(
        contract: String, owner: String, tokenId: BigInteger, cursor: String?, fn: suspend (String, String, String?) -> ImmutablexAssetsPage
    ): ImmutablexAsset? {
        val page = fn(contract, owner, cursor)
        val asset: ImmutablexAsset? = page.result.find { it.tokenId == String(tokenId.toByteArray()) }
        return if(asset == null && page.cursor.isNotEmpty()) {
            getAssetsByCollection(contract, owner, tokenId, page.cursor, fn)
        } else asset
    }

    private suspend fun convert(asset: ImmutablexAsset): UnionOwnership {
        val creator = client.getMints(pageSize = 1, itemId = asset.itemId).result.first().user
        return UnionOwnership(
            id = OwnershipIdDto(BlockchainDto.IMMUTABLEX, asset.tokenAddress, asset.tokenId(), asset.user!!),
            collection = CollectionIdDto(BlockchainDto.IMMUTABLEX, asset.tokenAddress),
            value = BigInteger.ONE,
            lazyValue = BigInteger.ZERO,
            createdAt = asset.createdAt!!,
            lastUpdatedAt = asset.updatedAt,
            creators = listOf(CreatorDto(account = UnionAddress(blockchain.group(), creator), 1))
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
