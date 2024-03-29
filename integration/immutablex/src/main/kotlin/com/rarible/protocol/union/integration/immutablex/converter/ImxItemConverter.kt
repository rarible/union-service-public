package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.core.common.nowMillis
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexAsset
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexMint
import java.math.BigDecimal
import java.math.BigInteger

object ImxItemConverter {

    private val logger by Logger()

    private val royaltyTypes = setOf("royalty")

    fun convert(asset: ImmutablexAsset, creator: String?, blockchain: BlockchainDto): UnionItem {
        return try {
            convertInternal(asset, creator, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Item: {} \n{}", blockchain, e.message, asset)
            throw e
        }
    }

    private fun convertInternal(
        asset: ImmutablexAsset,
        creator: String?,
        blockchain: BlockchainDto
    ): UnionItem {
        // val deleted = asset.user!! == "${Address.ZERO()}"
        val deleted = asset.isDeleted()
        val creatorAddress = toCreator(creator, blockchain)

        return UnionItem(
            id = ItemIdDto(blockchain, asset.encodedItemId()),
            collection = CollectionIdDto(blockchain, asset.tokenAddress),
            creators = listOfNotNull(creatorAddress),
            lazySupply = BigInteger.ZERO,
            deleted = deleted,
            supply = if (deleted) BigInteger.ZERO else BigInteger.ONE,
            mintedAt = asset.createdAt ?: asset.updatedAt ?: nowMillis(),
            lastUpdatedAt = asset.updatedAt ?: asset.createdAt ?: nowMillis(),
        )
    }

    fun convert(mint: ImmutablexMint, creator: String?, blockchain: BlockchainDto): UnionItem {
        return try {
            convertInternal(mint, creator, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Mint to Item: {} \n{}", blockchain, e.message, mint)
            throw e
        }
    }

    private fun convertInternal(mint: ImmutablexMint, creator: String?, blockchain: BlockchainDto): UnionItem {
        val creatorAddress = toCreator(creator, blockchain)
        return UnionItem(
            id = ItemIdDto(blockchain, mint.token.data.encodedItemId()),
            collection = CollectionIdDto(blockchain, mint.token.data.tokenAddress),
            creators = listOfNotNull(creatorAddress),
            lazySupply = BigInteger.ZERO,
            supply = BigInteger.ONE,
            mintedAt = mint.timestamp,
            lastUpdatedAt = mint.timestamp,
            deleted = false
        )
    }

    fun convertToRoyaltyDto(asset: ImmutablexAsset, blockchain: BlockchainDto): List<RoyaltyDto> =
        asset.fees.filter { royaltyTypes.contains(it.type) }.map {
            RoyaltyDto(
                account = UnionAddressConverter.convert(blockchain, it.address),
                value = it.percentage.multiply(BigDecimal(100)).toInt()
            )
        }

    private fun toCreator(creator: String?, blockchain: BlockchainDto) = creator?.let {
        CreatorDto(
            UnionAddressConverter.convert(blockchain, creator), 10000
        )
    }
}
