package com.rarible.protocol.union.integration.tezos.dipdup.converter

import com.rarible.dipdup.client.core.model.DipDupActivity
import com.rarible.dipdup.client.core.model.DipDupBurnActivity
import com.rarible.dipdup.client.core.model.DipDupCancelBidActivity
import com.rarible.dipdup.client.core.model.DipDupCancelFloorBidActivity
import com.rarible.dipdup.client.core.model.DipDupGetBidActivity
import com.rarible.dipdup.client.core.model.DipDupGetFloorBidActivity
import com.rarible.dipdup.client.core.model.DipDupMakeBidActivity
import com.rarible.dipdup.client.core.model.DipDupMakeFloorBidActivity
import com.rarible.dipdup.client.core.model.DipDupMintActivity
import com.rarible.dipdup.client.core.model.DipDupOrderCancelActivity
import com.rarible.dipdup.client.core.model.DipDupOrderListActivity
import com.rarible.dipdup.client.core.model.DipDupOrderSellActivity
import com.rarible.dipdup.client.core.model.DipDupTransferActivity
import com.rarible.dipdup.client.core.model.TezosPlatform
import com.rarible.dipdup.client.model.DipDupActivityType
import com.rarible.dipdup.client.model.DipDupSyncSort
import com.rarible.dipdup.client.model.DipDupSyncType
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionActivityDto
import com.rarible.protocol.union.core.model.UnionBurnActivityDto
import com.rarible.protocol.union.core.model.UnionMintActivityDto
import com.rarible.protocol.union.core.model.UnionOrderBidActivityDto
import com.rarible.protocol.union.core.model.UnionOrderCancelBidActivityDto
import com.rarible.protocol.union.core.model.UnionOrderCancelListActivityDto
import com.rarible.protocol.union.core.model.UnionOrderListActivityDto
import com.rarible.protocol.union.core.model.UnionOrderMatchSellDto
import com.rarible.protocol.union.core.model.UnionTransferActivityDto
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderActivitySourceDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.SyncTypeDto
import com.rarible.tzkt.utils.Tezos
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext

@Component
class DipDupActivityConverter(
    private val currencyService: CurrencyService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun convert(source: DipDupActivity, blockchain: BlockchainDto): UnionActivityDto {
        try {
            return convertInternal(source, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Activity: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    suspend fun convertLegacy(source: DipDupActivity, blockchain: BlockchainDto): UnionActivityDto {
        try {
            return convertInternalLegacy(source, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Legacy activity: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    fun convertToDipDupOrderActivitiesTypes(source: List<ActivityTypeDto>): List<DipDupActivityType> {
        return source.flatMap { asOrderActivityType(it) }
    }

    fun convertToDipDupNftActivitiesTypes(source: List<ActivityTypeDto>): List<DipDupActivityType> {
        return source.mapNotNull { asNftActivityType(it) }
    }

    fun asOrderActivityType(source: ActivityTypeDto) = when (source) {
        ActivityTypeDto.LIST -> listOf(DipDupActivityType.LIST)
        ActivityTypeDto.SELL -> listOf(DipDupActivityType.SELL, DipDupActivityType.GET_BID)
        ActivityTypeDto.CANCEL_LIST -> listOf(DipDupActivityType.CANCEL_LIST)
        ActivityTypeDto.BID -> listOf(DipDupActivityType.MAKE_BID)
        ActivityTypeDto.CANCEL_BID -> listOf(DipDupActivityType.CANCEL_BID)
        else -> emptyList()
    }


    fun asNftActivityType(source: ActivityTypeDto): DipDupActivityType? {
        return when (source) {
            ActivityTypeDto.TRANSFER -> DipDupActivityType.TRANSFER
            ActivityTypeDto.MINT -> DipDupActivityType.MINT
            ActivityTypeDto.BURN -> DipDupActivityType.BURN
            else -> null
        }
    }

    fun price(value: BigDecimal, makeValue: BigDecimal, platform: TezosPlatform) =
        try {
            value.divide(makeValue, MathContext.DECIMAL128)
        } catch (e: Exception) {
            logger.error("Failed to calculate price: $value", e)
            value
        }

    private suspend fun convertInternal(activity: DipDupActivity, blockchain: BlockchainDto): UnionActivityDto {
        val activityId = ActivityIdDto(blockchain, activity.id)
        val date = activity.date.toInstant()

        return when (activity) {
            is DipDupOrderListActivity -> {
                val make = DipDupConverter.convert(activity.make, blockchain)
                val take = DipDupConverter.convert(activity.take, blockchain)
                val price = price(activity.take.assetValue, activity.make.assetValue, activity.source)

                UnionOrderListActivityDto(
                    id = activityId,
                    orderId = OrderIdDto(blockchain, activity.orderId),
                    date = date,
                    price = price,
                    priceUsd = currencyService.toUsd(blockchain, take.type, make.value),
                    source = convert(activity.source),
                    hash = activity.hash,
                    maker = UnionAddressConverter.convert(blockchain, activity.maker),
                    make = make,
                    take = take,
                    reverted = activity.reverted
                )
            }
            is DipDupOrderCancelActivity -> {
                val make = DipDupConverter.convert(activity.make.assetType, blockchain)
                val take = DipDupConverter.convert(activity.take.assetType, blockchain)

                UnionOrderCancelListActivityDto(
                    id = activityId,
                    orderId = OrderIdDto(blockchain, activity.orderId),
                    date = date,
                    source = convert(activity.source),
                    hash = activity.hash,
                    maker = UnionAddressConverter.convert(blockchain, activity.maker),
                    reverted = false,
                    make = make,
                    take = take,
                    transactionHash = activity.hash
                )
            }
            is DipDupOrderSellActivity -> {
                val nft = DipDupConverter.convert(activity.nft, blockchain)
                val payment = DipDupConverter.convert(activity.payment, blockchain)

                UnionOrderMatchSellDto(
                    id = activityId,
                    orderId = OrderIdDto(blockchain, activity.orderId),
                    date = date,
                    source = convert(activity.source),
                    reverted = false,
                    transactionHash = activity.hash,
                    seller = UnionAddressConverter.convert(blockchain, activity.seller),
                    nft = nft,
                    payment = payment,
                    buyer = UnionAddressConverter.convert(blockchain, activity.buyer),
                    price = price(payment.value, nft.value, activity.source),
                    priceUsd = currencyService.toUsd(blockchain, payment.type, payment.value),
                    type = UnionOrderMatchSellDto.Type.SELL
                )
            }
            is DipDupMintActivity -> {
                val id = ActivityIdDto(blockchain, activity.id)
                UnionMintActivityDto(
                    id = id,
                    date = date,
                    reverted = activity.reverted,
                    owner = UnionAddressConverter.convert(blockchain, activity.owner),
                    transactionHash = activity.transactionId,
                    value = convertValue(activity.value, id),
                    tokenId = activity.tokenId,
                    itemId = ItemIdDto(blockchain, activity.contract, activity.tokenId),
                    contract = ContractAddress(blockchain, activity.contract),
                    collection = CollectionIdDto(blockchain, activity.contract)
                )
            }
            is DipDupTransferActivity -> {
                val id = ActivityIdDto(blockchain, activity.id)

                // Workaround for non-formal burn
                if (Tezos.NULL_ADDRESSES.contains(activity.owner)) {
                    UnionBurnActivityDto(
                        id = id,
                        date = date,
                        reverted = activity.reverted,
                        owner = UnionAddressConverter.convert(blockchain, activity.owner),
                        transactionHash = activity.transactionId,
                        value = convertValue(activity.value, id),
                        tokenId = activity.tokenId,
                        itemId = ItemIdDto(blockchain, activity.contract, activity.tokenId),
                        contract = ContractAddress(blockchain, activity.contract),
                        collection = CollectionIdDto(blockchain, activity.contract)
                    )
                } else {
                    UnionTransferActivityDto(
                        id = id,
                        date = date,
                        reverted = activity.reverted,
                        from = UnionAddressConverter.convert(blockchain, activity.from),
                        owner = UnionAddressConverter.convert(blockchain, activity.owner),
                        transactionHash = activity.transactionId,
                        value = convertValue(activity.value, id),
                        tokenId = activity.tokenId,
                        itemId = ItemIdDto(blockchain, activity.contract, activity.tokenId),
                        contract = ContractAddress(blockchain, activity.contract),
                        collection = CollectionIdDto(blockchain, activity.contract)
                    )
                }
            }
            is DipDupBurnActivity -> {
                val id = ActivityIdDto(blockchain, activity.id)
                UnionBurnActivityDto(
                    id = id,
                    date = date,
                    reverted = activity.reverted,
                    owner = UnionAddressConverter.convert(blockchain, activity.owner),
                    transactionHash = activity.transactionId,
                    value = convertValue(activity.value, id),
                    tokenId = activity.tokenId,
                    itemId = ItemIdDto(blockchain, activity.contract, activity.tokenId),
                    contract = ContractAddress(blockchain, activity.contract),
                    collection = CollectionIdDto(blockchain, activity.contract)
                )
            }
            is DipDupGetBidActivity -> {
                val nft = DipDupConverter.convert(activity.nft, blockchain)
                val payment = DipDupConverter.convert(activity.payment, blockchain)

                UnionOrderMatchSellDto(
                    id = activityId,
                    orderId = OrderIdDto(blockchain, activity.orderId),
                    date = date,
                    source = convert(activity.source),
                    reverted = false,
                    transactionHash = activity.hash,
                    seller = UnionAddressConverter.convert(blockchain, activity.seller),
                    nft = nft,
                    payment = payment,
                    buyer = UnionAddressConverter.convert(blockchain, activity.buyer),
                    price = price(payment.value, nft.value, activity.source),
                    priceUsd = currencyService.toUsd(blockchain, payment.type, payment.value),
                    type = UnionOrderMatchSellDto.Type.ACCEPT_BID
                )
            }
            is DipDupMakeBidActivity -> {
                val nft = DipDupConverter.convert(activity.take, blockchain)
                val payment = DipDupConverter.convert(activity.make, blockchain)

                UnionOrderBidActivityDto(
                    id = activityId,
                    orderId = OrderIdDto(blockchain, activity.orderId),
                    date = date,
                    price = activity.price,
                    priceUsd = currencyService.toUsd(blockchain, payment.type, payment.value),
                    hash = activity.hash,
                    maker = UnionAddressConverter.convert(blockchain, activity.maker),
                    make = payment,
                    take = nft,
                    reverted = false
                )
            }
            is DipDupCancelBidActivity -> {
                val nft = DipDupConverter.convert(activity.take, blockchain)
                val payment = DipDupConverter.convert(activity.make, blockchain)

                UnionOrderCancelBidActivityDto(
                    id = activityId,
                    orderId = OrderIdDto(blockchain, activity.orderId),
                    date = date,
                    hash = activity.hash,
                    maker = UnionAddressConverter.convert(blockchain, activity.maker),
                    make = payment.type,
                    take = nft.type,
                    transactionHash = activity.hash
                )
            }
            is DipDupMakeFloorBidActivity -> TODO()
            is DipDupGetFloorBidActivity -> TODO()
            is DipDupCancelFloorBidActivity -> TODO()
        }
    }

    private suspend fun convertInternalLegacy(activity: DipDupActivity, blockchain: BlockchainDto): UnionActivityDto {
        val activityId = ActivityIdDto(blockchain, activity.id)
        val date = activity.date.toInstant()

        return when (activity) {
            is DipDupOrderListActivity -> {
                val make = DipDupConverter.convert(activity.make, blockchain)
                val take = DipDupConverter.convert(activity.take, blockchain)
                val price = price(activity.take.assetValue, activity.make.assetValue, activity.source)

                UnionOrderListActivityDto(
                    id = activityId,
                    date = date,
                    price = price,
                    priceUsd = currencyService.toUsd(blockchain, take.type, make.value),
                    source = convert(activity.source),
                    hash = activity.hash,
                    maker = UnionAddressConverter.convert(blockchain, activity.maker),
                    make = make,
                    take = take,
                    reverted = activity.reverted
                )
            }
            is DipDupOrderCancelActivity -> {
                val make = DipDupConverter.convert(activity.make.assetType, blockchain)
                val take = DipDupConverter.convert(activity.take.assetType, blockchain)

                UnionOrderCancelListActivityDto(
                    id = activityId,
                    date = date,
                    source = convert(activity.source),
                    hash = activity.hash,
                    maker = UnionAddressConverter.convert(blockchain, activity.maker),
                    reverted = false,
                    make = make,
                    take = take,
                    transactionHash = activity.hash
                )
            }
            is DipDupOrderSellActivity -> {
                val nft = DipDupConverter.convert(activity.nft, blockchain)
                val payment = DipDupConverter.convert(activity.payment, blockchain)

                UnionOrderMatchSellDto(
                    id = activityId,
                    date = date,
                    source = convert(activity.source),
                    reverted = false,
                    transactionHash = activity.hash,
                    seller = UnionAddressConverter.convert(blockchain, activity.seller),
                    nft = nft,
                    payment = payment,
                    buyer = UnionAddressConverter.convert(blockchain, activity.buyer),
                    price = price(payment.value, nft.value, activity.source),
                    priceUsd = currencyService.toUsd(blockchain, payment.type, payment.value),
                    type = UnionOrderMatchSellDto.Type.SELL
                )
            }
            is DipDupGetBidActivity -> {
                val nft = DipDupConverter.convert(activity.nft, blockchain)
                val payment = DipDupConverter.convert(activity.payment, blockchain)

                UnionOrderMatchSellDto(
                    id = activityId,
                    date = date,
                    source = convert(activity.source),
                    reverted = false,
                    transactionHash = activity.hash,
                    seller = UnionAddressConverter.convert(blockchain, activity.seller),
                    nft = nft,
                    payment = payment,
                    buyer = UnionAddressConverter.convert(blockchain, activity.buyer),
                    price = price(payment.value, nft.value, activity.source),
                    priceUsd = currencyService.toUsd(blockchain, payment.type, payment.value),
                    type = UnionOrderMatchSellDto.Type.ACCEPT_BID
                )
            }
            is DipDupMintActivity -> {
                val id = ActivityIdDto(blockchain, activity.transferId)
                UnionMintActivityDto(
                    id = id,
                    date = date,
                    reverted = activity.reverted,
                    owner = UnionAddressConverter.convert(blockchain, activity.owner),
                    transactionHash = activity.transactionId,
                    value = convertValue(activity.value, id),
                    tokenId = activity.tokenId,
                    itemId = ItemIdDto(blockchain, activity.contract, activity.tokenId),
                    contract = ContractAddress(blockchain, activity.contract),
                    collection = CollectionIdDto(blockchain, activity.contract)
                )
            }
            is DipDupTransferActivity -> {
                val id = ActivityIdDto(blockchain, activity.transferId)

                // Workaround for non-formal burn
                if (Tezos.NULL_ADDRESSES.contains(activity.owner)) {
                    UnionBurnActivityDto(
                        id = id,
                        date = date,
                        reverted = activity.reverted,
                        owner = UnionAddressConverter.convert(blockchain, activity.owner),
                        transactionHash = activity.transactionId,
                        value = convertValue(activity.value, id),
                        tokenId = activity.tokenId,
                        itemId = ItemIdDto(blockchain, activity.contract, activity.tokenId),
                        contract = ContractAddress(blockchain, activity.contract),
                        collection = CollectionIdDto(blockchain, activity.contract)
                    )
                } else {
                    UnionTransferActivityDto(
                        id = id,
                        date = date,
                        reverted = activity.reverted,
                        from = UnionAddressConverter.convert(blockchain, activity.from),
                        owner = UnionAddressConverter.convert(blockchain, activity.owner),
                        transactionHash = activity.transactionId,
                        value = convertValue(activity.value, id),
                        tokenId = activity.tokenId,
                        itemId = ItemIdDto(blockchain, activity.contract, activity.tokenId),
                        contract = ContractAddress(blockchain, activity.contract),
                        collection = CollectionIdDto(blockchain, activity.contract)
                    )
                }
            }
            is DipDupBurnActivity -> {
                val id = ActivityIdDto(blockchain, activity.transferId)
                UnionBurnActivityDto(
                    id = id,
                    date = date,
                    reverted = activity.reverted,
                    owner = UnionAddressConverter.convert(blockchain, activity.owner),
                    transactionHash = activity.transactionId,
                    value = convertValue(activity.value, id),
                    tokenId = activity.tokenId,
                    itemId = ItemIdDto(blockchain, activity.contract, activity.tokenId),
                    contract = ContractAddress(blockchain, activity.contract),
                    collection = CollectionIdDto(blockchain, activity.contract)
                )
            }
            is DipDupMakeBidActivity -> {
                val nft = DipDupConverter.convert(activity.take, blockchain)
                val payment = DipDupConverter.convert(activity.make, blockchain)

                UnionOrderBidActivityDto(
                    id = activityId,
                    date = date,
                    price = activity.price,
                    priceUsd = currencyService.toUsd(blockchain, payment.type, payment.value),
                    hash = activity.hash,
                    maker = UnionAddressConverter.convert(blockchain, activity.maker),
                    make = payment,
                    take = nft,
                    reverted = false
                )
            }
            is DipDupCancelBidActivity -> {
                val nft = DipDupConverter.convert(activity.take, blockchain)
                val payment = DipDupConverter.convert(activity.make, blockchain)

                UnionOrderCancelBidActivityDto(
                    id = activityId,
                    date = date,
                    hash = activity.hash,
                    maker = UnionAddressConverter.convert(blockchain, activity.maker),
                    make = payment.type,
                    take = nft.type,
                    transactionHash = activity.hash
                )
            }
            is DipDupMakeFloorBidActivity -> TODO()
            is DipDupGetFloorBidActivity -> TODO()
            is DipDupCancelFloorBidActivity -> TODO()
        }
    }

    private fun convertValue(bd: BigDecimal, id: ActivityIdDto): BigInteger {
        if (bd.stripTrailingZeros().scale() > 0) {
            logger.warn("Value: $bd must be BigInteger for token activity: $id, tring to multiply by 1_000_000")
            return bd.multiply(BigDecimal(1_000_000)).toBigInteger()
        } else return bd.toBigInteger()
    }

    private fun convert(source: TezosPlatform): OrderActivitySourceDto {
        return when(source) {
            TezosPlatform.HEN -> OrderActivitySourceDto.HEN
            TezosPlatform.OBJKT_V1, TezosPlatform.OBJKT_V2 -> OrderActivitySourceDto.OBJKT
            TezosPlatform.RARIBLE_V1, TezosPlatform.RARIBLE_V2 -> OrderActivitySourceDto.RARIBLE
            TezosPlatform.TEIA_V1 -> OrderActivitySourceDto.TEIA
            TezosPlatform.VERSUM_V1 -> OrderActivitySourceDto.VERSUM
            TezosPlatform.FXHASH_V1, TezosPlatform.FXHASH_V2 -> OrderActivitySourceDto.FXHASH
            else -> throw RuntimeException("Not implemented for ${source} platform")
        }
    }

    companion object {
        fun convert(source: SyncSortDto): DipDupSyncSort =
            when (source) {
                SyncSortDto.DB_UPDATE_ASC -> DipDupSyncSort.DB_UPDATE_ASC
                SyncSortDto.DB_UPDATE_DESC -> DipDupSyncSort.DB_UPDATE_DESC
            }

        fun convert(source: SyncTypeDto): DipDupSyncType =
            when (source) {
                SyncTypeDto.ORDER -> DipDupSyncType.ORDER
                SyncTypeDto.NFT -> DipDupSyncType.NFT
                SyncTypeDto.AUCTION -> DipDupSyncType.AUCTION
            }
    }
}

