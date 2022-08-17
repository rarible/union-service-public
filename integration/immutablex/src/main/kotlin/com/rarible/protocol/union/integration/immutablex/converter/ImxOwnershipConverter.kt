package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.core.common.nowMillis
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexAsset
import java.math.BigInteger
import java.time.Instant

object ImxOwnershipConverter {

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
        return toOwnership(
            blockchain = blockchain,
            itemId = asset.encodedItemId(),
            owner = asset.user!!,
            creator = creator,
            createdAt = asset.createdAt ?: asset.updatedAt,
            updatedAt = asset.updatedAt
        )
    }

    fun toOwnership(
        blockchain: BlockchainDto,
        itemId: String,
        owner: String,
        creator: String?,
        createdAt: Instant?,
        updatedAt: Instant? = null
    ): UnionOwnership {
        val collection = IdParser.split(itemId, 2)[0]
        val creatorAddress = creator?.let { UnionAddressConverter.convert(blockchain, creator) }
        val ownerAddress = UnionAddressConverter.convert(blockchain, owner)
        return UnionOwnership(
            id = OwnershipIdDto(blockchain, itemId, ownerAddress),
            collection = CollectionIdDto(blockchain, collection),
            value = BigInteger.ONE,
            createdAt = createdAt ?: nowMillis(),
            lastUpdatedAt = updatedAt ?: createdAt ?: nowMillis(),
            lazyValue = BigInteger.ZERO,
            creators = listOfNotNull(creatorAddress?.let { CreatorDto(creatorAddress, 1) })
        )
    }
}
