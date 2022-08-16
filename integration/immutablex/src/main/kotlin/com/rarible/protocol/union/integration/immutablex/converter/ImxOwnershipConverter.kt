package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.core.common.nowMillis
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.OrderActivityMatchSideDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.OrderMatchSwapDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.ext
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexAsset
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTransfer
import java.math.BigInteger

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
        val creatorAddress = creator?.let { UnionAddressConverter.convert(blockchain, creator) }
        val ownerAddress = UnionAddressConverter.convert(blockchain, asset.user!!)
        return UnionOwnership(
            id = OwnershipIdDto(blockchain, asset.encodedItemId(), ownerAddress),
            collection = CollectionIdDto(blockchain, asset.tokenAddress),
            value = BigInteger.ONE,
            lazyValue = BigInteger.ZERO,
            createdAt = asset.createdAt ?: asset.updatedAt ?: nowMillis(),
            lastUpdatedAt = asset.updatedAt,
            creators = listOfNotNull(creatorAddress?.let { CreatorDto(creatorAddress, 1) })
        )
    }

    fun convert(transfer: ImmutablexTransfer, creator: String?, blockchain: BlockchainDto): UnionOwnership {
        return try {
            convertInternal(transfer, creator, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Ownership: {} \n{}", blockchain, e.message, transfer)
            throw e
        }
    }

    private fun convertInternal(
        transfer: ImmutablexTransfer,
        creator: String?,
        blockchain: BlockchainDto
    ): UnionOwnership {
        val creatorAddress = creator?.let { UnionAddressConverter.convert(blockchain, creator) }
        return UnionOwnership(
            id = OwnershipIdDto(
                blockchain,
                transfer.token.data.encodedItemId(),
                UnionAddressConverter.convert(blockchain, transfer.receiver)
            ),
            collection = CollectionIdDto(blockchain, transfer.token.data.tokenAddress),
            value = transfer.token.data.quantity,
            createdAt = transfer.timestamp,
            lastUpdatedAt = transfer.timestamp,
            lazyValue = BigInteger.ZERO,
            creators = listOfNotNull(creatorAddress?.let { CreatorDto(creatorAddress, 1) })
        )
    }

    fun convert(sell: OrderMatchSellDto, creator: String?, blockchain: BlockchainDto): UnionOwnership {
        return try {
            convertInternal(sell, creator, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Ownership: {} \n{}", blockchain, e.message, sell)
            throw e
        }
    }

    private fun convertInternal(
        sell: OrderMatchSellDto,
        creator: String?,
        blockchain: BlockchainDto
    ): UnionOwnership {
        val creatorAddress = creator?.let { UnionAddressConverter.convert(blockchain, creator) }
        val itemId = sell.nft.type.ext.itemId!!
        val collection = itemId.value.substringBefore(":")
        return UnionOwnership(
            id = itemId.toOwnership(sell.buyer.value),
            collection = CollectionIdDto(blockchain, collection),
            value = BigInteger.ONE,
            createdAt = sell.date,
            lastUpdatedAt = sell.lastUpdatedAt,
            lazyValue = BigInteger.ZERO,
            creators = listOfNotNull(creatorAddress?.let { CreatorDto(creatorAddress, 1) })
        )
    }

    fun convert(
        swap: OrderMatchSwapDto,
        side: OrderActivityMatchSideDto,
        creator: String?,
        blockchain: BlockchainDto
    ): UnionOwnership {
        return try {
            convertInternal(swap, side, creator, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Ownership: {} \n{}", blockchain, e.message, swap)
            throw e
        }
    }

    private fun convertInternal(
        swap: OrderMatchSwapDto,
        side: OrderActivityMatchSideDto,
        creator: String?,
        blockchain: BlockchainDto
    ): UnionOwnership {
        val creatorAddress = creator?.let { UnionAddressConverter.convert(blockchain, creator) }
        val itemId = side.asset.type.ext.itemId!!
        val collection = itemId.value.substringBefore(":")
        return UnionOwnership(
            id = itemId.toOwnership(side.maker.value),
            collection = CollectionIdDto(blockchain, collection),
            value = BigInteger.ONE,
            createdAt = swap.date,
            lastUpdatedAt = swap.lastUpdatedAt,
            lazyValue = BigInteger.ZERO,
            creators = listOfNotNull(creatorAddress?.let { CreatorDto(creatorAddress, 1) })
        )
    }

}
