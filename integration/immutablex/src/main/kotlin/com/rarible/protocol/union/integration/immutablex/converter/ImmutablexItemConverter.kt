package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.core.common.nowMillis
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexApiClient
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexAsset
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger

class ImmutablexItemConverter(
    private val client: ImmutablexApiClient
) {

    private val logger by Logger()

    suspend fun convert(asset: ImmutablexAsset, blockchain: BlockchainDto): UnionItem {
        return try {
            convertInternal(asset, blockchain)
        } catch (e: Exception) {
            logger.error("Convert immutable x asset \n$asset\n is failed! \n${e.message}", e)
            throw e
        }
    }

    private suspend fun convertInternal(asset: ImmutablexAsset, blockchain: BlockchainDto): UnionItem {
        val deleted = asset.user!! == "${Address.ZERO()}"
        // TODO IMMUTABLEX Performance!!!
        val creator = client.getMints(pageSize = 1, itemId = asset.itemId).result.firstOrNull()?.user
        return UnionItem(
            id = ItemIdDto(BlockchainDto.IMMUTABLEX, asset.itemId),
            collection = CollectionIdDto(blockchain, asset.tokenAddress),
            creators = if (creator == null) emptyList() else listOf(
                CreatorDto(account = UnionAddressConverter.convert(blockchain, creator), 1)
            ),
            lazySupply = BigInteger.ZERO,
            deleted = deleted,
            supply = if (deleted) BigInteger.ZERO else BigInteger.ONE,
            mintedAt = asset.createdAt ?: nowMillis(),
            lastUpdatedAt = asset.updatedAt ?: nowMillis(),
        )
    }

    fun convertToRoyaltyDto(asset: ImmutablexAsset, blockchain: BlockchainDto): List<RoyaltyDto> =
        asset.fees.map {
            RoyaltyDto(
                account = UnionAddressConverter.convert(ContractAddressConverter.convert(blockchain, it.address)),
                value = it.percentage.multiply(BigDecimal(100)).toInt()
            )
        }
}
