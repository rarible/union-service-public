package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexAsset
import java.math.BigInteger

object ImmutablexOwnershipConverter {

    private val logger by Logger()

    fun convert(
        assets: Collection<ImmutablexAsset>,
        creators: Map<String, String>,
        blockchain: BlockchainDto
    ): List<UnionOwnership> {
        return assets.map { convert(it, creators[it.itemId], blockchain) }
    }

    fun convert(asset: ImmutablexAsset, creator: String?, blockchain: BlockchainDto): UnionOwnership {
        return try {
            convertInternal(asset, creator, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Ownership: {} \n{}", blockchain, e.message, asset)
            throw e
        }
    }

    private fun convertInternal(
        asset: ImmutablexAsset,
        creator: String?,
        blockchain: BlockchainDto
    ): UnionOwnership {
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

}