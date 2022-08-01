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
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexAsset
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger

object ImmutablexItemConverter {

    private val logger by Logger()

    fun convert(
        assets: Collection<ImmutablexAsset>,
        creators: Map<String, String>,
        blockchain: BlockchainDto
    ): List<UnionItem> {
        return assets.map { convert(it, creators[it.itemId], blockchain) }
    }

    fun convert(asset: ImmutablexAsset, creator: String?, blockchain: BlockchainDto): UnionItem {
        return try {
            convertInternal(asset, creator, blockchain)
        } catch (e: Exception) {
            logger.error("Convert immutable x asset \n$asset\n is failed! \n${e.message}", e)
            throw e
        }
    }

    private fun convertInternal(
        asset: ImmutablexAsset,
        creator: String?,
        blockchain: BlockchainDto
    ): UnionItem {
        val deleted = asset.user!! == "${Address.ZERO()}"
        val creatorAddress = creator?.let { CreatorDto(UnionAddressConverter.convert(blockchain, creator), 1) }

        return UnionItem(
            id = ItemIdDto(blockchain, asset.itemId),
            collection = CollectionIdDto(blockchain, asset.tokenAddress),
            creators = listOfNotNull(creatorAddress),
            lazySupply = BigInteger.ZERO,
            deleted = deleted,
            supply = if (deleted) BigInteger.ZERO else BigInteger.ONE,
            mintedAt = asset.createdAt ?: asset.updatedAt ?: nowMillis(),
            lastUpdatedAt = asset.updatedAt ?: asset.createdAt ?: nowMillis(),
        )
    }

    fun convertToRoyaltyDto(asset: ImmutablexAsset, blockchain: BlockchainDto): List<RoyaltyDto> =
        asset.fees.map {
            RoyaltyDto(
                account = UnionAddressConverter.convert(blockchain, it.address),
                value = it.percentage.multiply(BigDecimal(100)).toInt()
            )
        }
}
