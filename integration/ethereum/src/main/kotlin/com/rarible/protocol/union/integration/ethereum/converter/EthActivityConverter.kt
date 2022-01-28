package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.protocol.dto.ActivityFilterAllTypeDto
import com.rarible.protocol.dto.ActivityFilterByCollectionTypeDto
import com.rarible.protocol.dto.ActivityFilterByItemTypeDto
import com.rarible.protocol.dto.ActivityFilterByUserTypeDto
import com.rarible.protocol.dto.AuctionActivityBidDto
import com.rarible.protocol.dto.AuctionActivityCancelDto
import com.rarible.protocol.dto.AuctionActivityEndDto
import com.rarible.protocol.dto.AuctionActivityFilterAllDto
import com.rarible.protocol.dto.AuctionActivityFilterByCollectionDto
import com.rarible.protocol.dto.AuctionActivityFilterByItemDto
import com.rarible.protocol.dto.AuctionActivityFilterByUserDto
import com.rarible.protocol.dto.AuctionActivityFinishDto
import com.rarible.protocol.dto.AuctionActivityOpenDto
import com.rarible.protocol.dto.AuctionActivityStartDto
import com.rarible.protocol.dto.BurnDto
import com.rarible.protocol.dto.MintDto
import com.rarible.protocol.dto.NftActivityFilterAllDto
import com.rarible.protocol.dto.NftActivityFilterByCollectionDto
import com.rarible.protocol.dto.NftActivityFilterByItemDto
import com.rarible.protocol.dto.NftActivityFilterByUserDto
import com.rarible.protocol.dto.OrderActivityBidDto
import com.rarible.protocol.dto.OrderActivityCancelBidDto
import com.rarible.protocol.dto.OrderActivityCancelListDto
import com.rarible.protocol.dto.OrderActivityDto
import com.rarible.protocol.dto.OrderActivityFilterAllDto
import com.rarible.protocol.dto.OrderActivityFilterByCollectionDto
import com.rarible.protocol.dto.OrderActivityFilterByItemDto
import com.rarible.protocol.dto.OrderActivityFilterByUserDto
import com.rarible.protocol.dto.OrderActivityListDto
import com.rarible.protocol.dto.OrderActivityMatchDto
import com.rarible.protocol.dto.TransferDto
import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.ActivityBlockchainInfoDto
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.ActivityTypeDto.*
import com.rarible.protocol.union.dto.AuctionBidActivityDto
import com.rarible.protocol.union.dto.AuctionCancelActivityDto
import com.rarible.protocol.union.dto.AuctionEndActivityDto
import com.rarible.protocol.union.dto.AuctionFinishActivityDto
import com.rarible.protocol.union.dto.AuctionOpenActivityDto
import com.rarible.protocol.union.dto.AuctionStartActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderActivityMatchSideDto
import com.rarible.protocol.union.dto.OrderActivitySourceDto
import com.rarible.protocol.union.dto.OrderBidActivityDto
import com.rarible.protocol.union.dto.OrderCancelBidActivityDto
import com.rarible.protocol.union.dto.OrderCancelListActivityDto
import com.rarible.protocol.union.dto.OrderListActivityDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.OrderMatchSwapDto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.dto.ext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EthActivityConverter(
    private val currencyService: CurrencyService,
    private val auctionConverter: EthAuctionConverter
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun convert(source: com.rarible.protocol.dto.ActivityDto, blockchain: BlockchainDto): ActivityDto {
        try {
            return convertInternal(source, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Activity: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    // For activities, we decided to do NOT convert price to USD in realtime,
    // since we want to see here USD price for the moment when event happened
    // For example, if user bought NFT for 1 ETH 2 days ago, we want to see here USD price
    // for THAT date instead of price calculated for current rate
    private suspend fun convertInternal(
        source: com.rarible.protocol.dto.ActivityDto,
        blockchain: BlockchainDto
    ): ActivityDto {
        val activityId = ActivityIdDto(blockchain, source.id)
        return when (source) {
            is OrderActivityMatchDto -> {
                val type = source.type
                val leftSide = source.left
                val rightSide = source.right
                val leftTypeExt = EthConverter.convert(leftSide.asset.assetType, blockchain).ext
                val rightTypeExt = EthConverter.convert(rightSide.asset.assetType, blockchain).ext
                if (type != null && leftTypeExt.isNft && rightTypeExt.isCurrency) {
                    activityToSell(
                        activityId = activityId,
                        source = source,
                        blockchain = blockchain,
                        nft = leftSide,
                        payment = rightSide,
                        type = convert(type)
                    )
                } else if (type != null && leftTypeExt.isCurrency && rightTypeExt.isNft) {
                    activityToSell(
                        activityId = activityId,
                        source = source,
                        blockchain = blockchain,
                        nft = rightSide,
                        payment = leftSide,
                        type = convert(type)
                    )
                } else {
                    activityToSwap(
                        activityId = activityId,
                        source = source,
                        blockchain = blockchain
                    )
                }
            }
            is OrderActivityBidDto -> {
                val payment = EthConverter.convert(source.make, blockchain)
                val nft = EthConverter.convert(source.take, blockchain)
                OrderBidActivityDto(
                    id = activityId,
                    date = source.date,
                    price = source.price,
                    priceUsd = source.priceUsd,
                    //priceUsd = currencyService.toUsd(blockchain, payment.type.ext.contract, source.price),
                    source = convert(source.source),
                    hash = EthConverter.convert(source.hash),
                    maker = EthConverter.convert(source.maker, blockchain),
                    make = payment,
                    take = nft
                )
            }
            is OrderActivityListDto -> {
                val payment = EthConverter.convert(source.take, blockchain)
                val nft = EthConverter.convert(source.make, blockchain)
                OrderListActivityDto(
                    id = activityId,
                    date = source.date,
                    price = source.price,
                    priceUsd = source.priceUsd,
                    //priceUsd = currencyService.toUsd(blockchain, payment.type.ext.contract, source.price),
                    source = convert(source.source),
                    hash = EthConverter.convert(source.hash),
                    maker = EthConverter.convert(source.maker, blockchain),
                    make = nft,
                    take = payment
                )
            }
            is OrderActivityCancelBidDto -> {
                OrderCancelBidActivityDto(
                    id = activityId,
                    date = source.date,
                    source = convert(source.source),
                    hash = EthConverter.convert(source.hash),
                    maker = EthConverter.convert(source.maker, blockchain),
                    make = EthConverter.convert(source.make, blockchain),
                    take = EthConverter.convert(source.take, blockchain),
                    transactionHash = EthConverter.convert(source.transactionHash),
                    // TODO UNION remove in 1.19
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = EthConverter.convert(source.transactionHash),
                        blockHash = EthConverter.convert(source.blockHash),
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    )
                )
            }
            is OrderActivityCancelListDto -> {
                OrderCancelListActivityDto(
                    id = activityId,
                    date = source.date,
                    source = convert(source.source),
                    hash = EthConverter.convert(source.hash),
                    maker = EthConverter.convert(source.maker, blockchain),
                    make = EthConverter.convert(source.make, blockchain),
                    take = EthConverter.convert(source.take, blockchain),
                    transactionHash = EthConverter.convert(source.transactionHash),
                    // TODO UNION remove in 1.19
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = EthConverter.convert(source.transactionHash),
                        blockHash = EthConverter.convert(source.blockHash),
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    )
                )
            }
            is MintDto -> {
                MintActivityDto(
                    id = activityId,
                    date = source.date,
                    owner = EthConverter.convert(source.owner, blockchain),
                    contract = ContractAddressConverter.convert(blockchain, EthConverter.convert(source.contract)),
                    tokenId = source.tokenId,
                    value = source.value,
                    transactionHash = EthConverter.convert(source.transactionHash),
                    // TODO UNION remove in 1.19
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = EthConverter.convert(source.transactionHash),
                        blockHash = EthConverter.convert(source.blockHash),
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    )
                )
            }
            is BurnDto -> {
                BurnActivityDto(
                    id = activityId,
                    date = source.date,
                    owner = EthConverter.convert(source.owner, blockchain),
                    contract = ContractAddressConverter.convert(blockchain, EthConverter.convert(source.contract)),
                    tokenId = source.tokenId,
                    value = source.value,
                    transactionHash = EthConverter.convert(source.transactionHash),
                    // TODO UNION remove in 1.19
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = EthConverter.convert(source.transactionHash),
                        blockHash = EthConverter.convert(source.blockHash),
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    )
                )
            }
            is TransferDto -> {
                TransferActivityDto(
                    id = activityId,
                    date = source.date,
                    from = EthConverter.convert(source.from, blockchain),
                    owner = EthConverter.convert(source.owner, blockchain),
                    contract = ContractAddressConverter.convert(blockchain, EthConverter.convert(source.contract)),
                    tokenId = source.tokenId,
                    value = source.value,
                    transactionHash = EthConverter.convert(source.transactionHash),
                    // TODO UNION remove in 1.19
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = EthConverter.convert(source.transactionHash),
                        blockHash = EthConverter.convert(source.blockHash),
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    )
                )
            }
            is AuctionActivityOpenDto -> {
                AuctionOpenActivityDto(
                    id = activityId,
                    date = source.date,
                    auction = auctionConverter.convert(source.auction, blockchain),
                    transactionHash = EthConverter.convert(source.transactionHash),
                )
            }
            is AuctionActivityBidDto -> {
                AuctionBidActivityDto(
                    id = activityId,
                    date = source.date,
                    bid = EthConverter.convert(source.bid, blockchain),
                    auction = auctionConverter.convert(source.auction, blockchain),
                    transactionHash = EthConverter.convert(source.transactionHash),
                )
            }
            is AuctionActivityFinishDto -> {
                AuctionFinishActivityDto(
                    id = activityId,
                    date = source.date,
                    auction = auctionConverter.convert(source.auction, blockchain),
                    transactionHash = EthConverter.convert(source.transactionHash)
                )
            }
            is AuctionActivityCancelDto -> {
                AuctionCancelActivityDto(
                    id = activityId,
                    date = source.date,
                    auction = auctionConverter.convert(source.auction, blockchain),
                    transactionHash = EthConverter.convert(source.transactionHash),
                )
            }
            is AuctionActivityStartDto -> {
                AuctionStartActivityDto(
                    id = activityId,
                    date = source.date,
                    auction = auctionConverter.convert(source.auction, blockchain)
                )
            }
            is AuctionActivityEndDto -> {
                AuctionEndActivityDto(
                    id = activityId,
                    date = source.date,
                    auction = auctionConverter.convert(source.auction, blockchain)
                )
            }
        }
    }

    private suspend fun activityToSell(
        source: OrderActivityMatchDto,
        blockchain: BlockchainDto,
        nft: com.rarible.protocol.dto.OrderActivityMatchSideDto,
        payment: com.rarible.protocol.dto.OrderActivityMatchSideDto,
        type: OrderMatchSellDto.Type,
        activityId: ActivityIdDto
    ): OrderMatchSellDto {
        val unionPayment = EthConverter.convert(payment.asset, blockchain)
        val priceUsd = source.priceUsd
        //val priceUsd = currencyService.toUsd(blockchain, unionPayment.type.ext.contract, source.price)
        return OrderMatchSellDto(
            id = activityId,
            date = source.date,
            source = convert(source.source),
            transactionHash = EthConverter.convert(source.transactionHash),
            // TODO UNION remove in 1.19
            blockchainInfo = asActivityBlockchainInfo(source),
            nft = EthConverter.convert(nft.asset, blockchain),
            payment = unionPayment,
            seller = EthConverter.convert(nft.maker, blockchain),
            buyer = EthConverter.convert(payment.maker, blockchain),
            sellerOrderHash = EthConverter.convert(nft.hash),
            buyerOrderHash = EthConverter.convert(payment.hash),
            price = source.price,
            priceUsd = priceUsd,
            amountUsd = priceUsd?.multiply(nft.asset.valueDecimal),
            type = type
        )
    }

    private fun activityToSwap(
        source: OrderActivityMatchDto,
        blockchain: BlockchainDto,
        activityId: ActivityIdDto
    ) = OrderMatchSwapDto(
        id = activityId,
        date = source.date,
        source = convert(source.source),
        transactionHash = EthConverter.convert(source.transactionHash),
        // TODO UNION remove in 1.19
        blockchainInfo = asActivityBlockchainInfo(source),
        left = convert(source.left, blockchain),
        right = convert(source.right, blockchain)

    )

    fun convertToNftAllTypes(types: List<ActivityTypeDto>): List<NftActivityFilterAllDto.Types>? {
        val result = types.mapNotNull { asNftActivityAllType(it) }.distinct()
        return result.ifEmpty { null }
    }

    fun convertToNftCollectionTypes(types: List<ActivityTypeDto>): List<NftActivityFilterByCollectionDto.Types>? {
        val result = types.mapNotNull { asNftActivityCollectionType(it) }.distinct()
        return result.ifEmpty { null }
    }

    fun convertToNftItemTypes(types: List<ActivityTypeDto>): List<NftActivityFilterByItemDto.Types>? {
        val result = types.mapNotNull { asNftActivityItemType(it) }.distinct()
        return result.ifEmpty { null }
    }

    fun convertToNftUserTypes(types: List<UserActivityTypeDto>): List<NftActivityFilterByUserDto.Types>? {
        val result = types.mapNotNull { asNftActivityUserType(it) }.distinct()
        return result.ifEmpty { null }
    }

    fun asNftActivityAllType(source: ActivityTypeDto): NftActivityFilterAllDto.Types? {
        return when (source) {
            TRANSFER -> NftActivityFilterAllDto.Types.TRANSFER
            MINT -> NftActivityFilterAllDto.Types.MINT
            BURN -> NftActivityFilterAllDto.Types.BURN
            else -> null
        }
    }

    fun asNftActivityCollectionType(source: ActivityTypeDto): NftActivityFilterByCollectionDto.Types? {
        return when (source) {
            TRANSFER -> NftActivityFilterByCollectionDto.Types.TRANSFER
            MINT -> NftActivityFilterByCollectionDto.Types.MINT
            BURN -> NftActivityFilterByCollectionDto.Types.BURN
            else -> null
        }
    }

    fun asNftActivityItemType(source: ActivityTypeDto): NftActivityFilterByItemDto.Types? {
        return when (source) {
            TRANSFER -> NftActivityFilterByItemDto.Types.TRANSFER
            MINT -> NftActivityFilterByItemDto.Types.MINT
            BURN -> NftActivityFilterByItemDto.Types.BURN
            else -> null
        }
    }

    fun asNftActivityUserType(source: UserActivityTypeDto): NftActivityFilterByUserDto.Types? {
        return when (source) {
            UserActivityTypeDto.TRANSFER_FROM -> NftActivityFilterByUserDto.Types.TRANSFER_FROM
            UserActivityTypeDto.TRANSFER_TO -> NftActivityFilterByUserDto.Types.TRANSFER_TO
            UserActivityTypeDto.MINT -> NftActivityFilterByUserDto.Types.MINT
            UserActivityTypeDto.BURN -> NftActivityFilterByUserDto.Types.BURN
            else -> null
        }
    }

    fun convertToOrderAllTypes(types: List<ActivityTypeDto>): List<OrderActivityFilterAllDto.Types>? {
        val result = types.mapNotNull { asOrderActivityAllType(it) }.distinct()
        return result.ifEmpty { null }
    }

    fun convertToOrderCollectionTypes(types: List<ActivityTypeDto>): List<OrderActivityFilterByCollectionDto.Types>? {
        val result = types.mapNotNull { asOrderActivityCollectionType(it) }.distinct()
        return result.ifEmpty { null }
    }

    fun convertToOrderItemTypes(types: List<ActivityTypeDto>): List<OrderActivityFilterByItemDto.Types>? {
        val result = types.mapNotNull { asOrderActivityItemType(it) }.distinct()
        return result.ifEmpty { null }
    }

    fun convertToOrderUserTypes(types: List<UserActivityTypeDto>): List<OrderActivityFilterByUserDto.Types>? {
        val result = types.mapNotNull { asOrderActivityUserType(it) }.distinct()
        return result.ifEmpty { null }
    }

    fun asOrderActivityAllType(source: ActivityTypeDto): OrderActivityFilterAllDto.Types? {
        return when (source) {
            BID -> OrderActivityFilterAllDto.Types.BID
            LIST -> OrderActivityFilterAllDto.Types.LIST
            SELL -> OrderActivityFilterAllDto.Types.MATCH
            CANCEL_LIST -> OrderActivityFilterAllDto.Types.CANCEL_LIST
            CANCEL_BID -> OrderActivityFilterAllDto.Types.CANCEL_BID
            else -> null
        }
    }

    fun asOrderActivityCollectionType(source: ActivityTypeDto): OrderActivityFilterByCollectionDto.Types? {
        return when (source) {
            BID -> OrderActivityFilterByCollectionDto.Types.BID
            LIST -> OrderActivityFilterByCollectionDto.Types.LIST
            SELL -> OrderActivityFilterByCollectionDto.Types.MATCH
            CANCEL_BID -> OrderActivityFilterByCollectionDto.Types.CANCEL_BID
            CANCEL_LIST -> OrderActivityFilterByCollectionDto.Types.CANCEL_LIST
            else -> null
        }
    }

    fun asOrderActivityItemType(source: ActivityTypeDto): OrderActivityFilterByItemDto.Types? {
        return when (source) {
            BID -> OrderActivityFilterByItemDto.Types.BID
            LIST -> OrderActivityFilterByItemDto.Types.LIST
            SELL -> OrderActivityFilterByItemDto.Types.MATCH
            CANCEL_BID -> OrderActivityFilterByItemDto.Types.CANCEL_BID
            CANCEL_LIST -> OrderActivityFilterByItemDto.Types.CANCEL_LIST
            else -> null
        }
    }

    fun asOrderActivityUserType(source: UserActivityTypeDto): OrderActivityFilterByUserDto.Types? {
        return when (source) {
            UserActivityTypeDto.MAKE_BID -> OrderActivityFilterByUserDto.Types.MAKE_BID
            UserActivityTypeDto.GET_BID -> OrderActivityFilterByUserDto.Types.GET_BID
            UserActivityTypeDto.BUY -> OrderActivityFilterByUserDto.Types.BUY
            UserActivityTypeDto.LIST -> OrderActivityFilterByUserDto.Types.LIST
            UserActivityTypeDto.SELL -> OrderActivityFilterByUserDto.Types.SELL
            UserActivityTypeDto.CANCEL_BID -> OrderActivityFilterByUserDto.Types.CANCEL_BID
            UserActivityTypeDto.CANCEL_LIST -> OrderActivityFilterByUserDto.Types.CANCEL_LIST
            else -> null
        }
    }

    fun convertToAuctionAllTypes(types: List<ActivityTypeDto>): List<AuctionActivityFilterAllDto.Types>? {
        val result = types.mapNotNull { asAuctionActivityAllType(it) }.distinct()
        return result.ifEmpty { null }
    }

    fun convertToAuctionCollectionTypes(types: List<ActivityTypeDto>): List<AuctionActivityFilterByCollectionDto.Types>? {
        val result = types.mapNotNull { asAuctionActivityCollectionType(it) }.distinct()
        return result.ifEmpty { null }
    }

    fun convertToAuctionItemTypes(types: List<ActivityTypeDto>): List<AuctionActivityFilterByItemDto.Types>? {
        val result = types.mapNotNull { asAuctionActivityItemType(it) }.distinct()
        return result.ifEmpty { null }
    }

    fun convertToAuctionUserTypes(types: List<UserActivityTypeDto>): List<AuctionActivityFilterByUserDto.Types>? {
        val result = types.mapNotNull { asAuctionActivityUserType(it) }.distinct()
        return result.ifEmpty { null }
    }

    fun asAuctionActivityAllType(source: ActivityTypeDto): AuctionActivityFilterAllDto.Types? {
        return when (source) {
            BID -> AuctionActivityFilterAllDto.Types.BID
            CREATED_AUCTION -> AuctionActivityFilterAllDto.Types.CREATED
            CANCEL_AUCTION -> AuctionActivityFilterAllDto.Types.CANCEL
            FINISHED_AUCTION -> AuctionActivityFilterAllDto.Types.FINISHED
            STARTED_AUCTION -> AuctionActivityFilterAllDto.Types.STARTED
            ENDED_AUCTION -> AuctionActivityFilterAllDto.Types.ENDED
            else -> null
        }
    }

    fun asAuctionActivityCollectionType(source: ActivityTypeDto): AuctionActivityFilterByCollectionDto.Types? {
        return when (source) {
            BID -> AuctionActivityFilterByCollectionDto.Types.BID
            CREATED_AUCTION -> AuctionActivityFilterByCollectionDto.Types.CREATED
            CANCEL_AUCTION -> AuctionActivityFilterByCollectionDto.Types.CANCEL
            FINISHED_AUCTION -> AuctionActivityFilterByCollectionDto.Types.FINISHED
            STARTED_AUCTION -> AuctionActivityFilterByCollectionDto.Types.STARTED
            ENDED_AUCTION -> AuctionActivityFilterByCollectionDto.Types.ENDED
            else -> null
        }
    }

    fun asAuctionActivityItemType(source: ActivityTypeDto): AuctionActivityFilterByItemDto.Types? {
        return when (source) {
            BID -> AuctionActivityFilterByItemDto.Types.BID
            CREATED_AUCTION -> AuctionActivityFilterByItemDto.Types.CREATED
            CANCEL_AUCTION -> AuctionActivityFilterByItemDto.Types.CANCEL
            FINISHED_AUCTION -> AuctionActivityFilterByItemDto.Types.FINISHED
            STARTED_AUCTION -> AuctionActivityFilterByItemDto.Types.STARTED
            ENDED_AUCTION -> AuctionActivityFilterByItemDto.Types.ENDED
            else -> null
        }
    }

    fun asAuctionActivityUserType(source: UserActivityTypeDto): AuctionActivityFilterByUserDto.Types? {
        return when (source) {
            UserActivityTypeDto.MAKE_BID -> AuctionActivityFilterByUserDto.Types.BID
            UserActivityTypeDto.CREATED_AUCTION -> AuctionActivityFilterByUserDto.Types.CREATED
            UserActivityTypeDto.CANCEL_AUCTION -> AuctionActivityFilterByUserDto.Types.CANCEL
            else -> null
        }
    }

    private fun asActivityBlockchainInfo(source: OrderActivityMatchDto) = ActivityBlockchainInfoDto(
        transactionHash = EthConverter.convert(source.transactionHash),
        blockHash = EthConverter.convert(source.blockHash),
        blockNumber = source.blockNumber,
        logIndex = source.logIndex
    )

    private fun convert(
        source: com.rarible.protocol.dto.OrderActivityMatchSideDto,
        blockchain: BlockchainDto
    ): OrderActivityMatchSideDto {
        return OrderActivityMatchSideDto(
            maker = EthConverter.convert(source.maker, blockchain),
            hash = EthConverter.convert(source.hash),
            asset = EthConverter.convert(source.asset, blockchain)
        )
    }

    private fun convert(source: OrderActivityDto.Source): OrderActivitySourceDto {
        return when (source) {
            OrderActivityDto.Source.OPEN_SEA -> OrderActivitySourceDto.OPEN_SEA
            OrderActivityDto.Source.RARIBLE -> OrderActivitySourceDto.RARIBLE
            OrderActivityDto.Source.CRYPTO_PUNKS -> OrderActivitySourceDto.CRYPTO_PUNKS
        }
    }

    private fun convert(source: OrderActivityMatchDto.Type) =
        when (source) {
            OrderActivityMatchDto.Type.SELL -> OrderMatchSellDto.Type.SELL
            OrderActivityMatchDto.Type.ACCEPT_BID -> OrderMatchSellDto.Type.ACCEPT_BID
        }
}

