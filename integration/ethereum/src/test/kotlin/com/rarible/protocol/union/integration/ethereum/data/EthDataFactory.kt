package com.rarible.protocol.union.integration.ethereum.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBinary
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.dto.AssetDto
import com.rarible.protocol.dto.AuctionActivityBidDto
import com.rarible.protocol.dto.AuctionActivityCancelDto
import com.rarible.protocol.dto.AuctionActivityDto
import com.rarible.protocol.dto.AuctionActivityEndDto
import com.rarible.protocol.dto.AuctionActivityFinishDto
import com.rarible.protocol.dto.AuctionActivityOpenDto
import com.rarible.protocol.dto.AuctionActivityStartDto
import com.rarible.protocol.dto.AuctionBidDto
import com.rarible.protocol.dto.AuctionBidsPaginationDto
import com.rarible.protocol.dto.AuctionDto
import com.rarible.protocol.dto.AuctionHistoryDto
import com.rarible.protocol.dto.AuctionStatusDto
import com.rarible.protocol.dto.BurnDto
import com.rarible.protocol.dto.CollectionAssetTypeDto
import com.rarible.protocol.dto.CryptoPunkOrderDto
import com.rarible.protocol.dto.Erc1155AssetTypeDto
import com.rarible.protocol.dto.Erc20AssetTypeDto
import com.rarible.protocol.dto.Erc721AssetTypeDto
import com.rarible.protocol.dto.ItemRoyaltyDto
import com.rarible.protocol.dto.ItemTransferDto
import com.rarible.protocol.dto.LegacyOrderDto
import com.rarible.protocol.dto.MintDto
import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.dto.NftItemAttributeDto
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.NftMediaDto
import com.rarible.protocol.dto.NftMediaMetaDto
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.dto.OnChainOrderDto
import com.rarible.protocol.dto.OpenSeaV1OrderDto
import com.rarible.protocol.dto.OrderActivityBidDto
import com.rarible.protocol.dto.OrderActivityCancelBidDto
import com.rarible.protocol.dto.OrderActivityCancelListDto
import com.rarible.protocol.dto.OrderActivityDto
import com.rarible.protocol.dto.OrderActivityListDto
import com.rarible.protocol.dto.OrderActivityMatchDto
import com.rarible.protocol.dto.OrderActivityMatchSideDto
import com.rarible.protocol.dto.OrderCancelDto
import com.rarible.protocol.dto.OrderCryptoPunksDataDto
import com.rarible.protocol.dto.OrderDataLegacyDto
import com.rarible.protocol.dto.OrderOpenSeaV1DataV1Dto
import com.rarible.protocol.dto.OrderRaribleV2DataV1Dto
import com.rarible.protocol.dto.OrderSideDto
import com.rarible.protocol.dto.OrderSideMatchDto
import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.dto.RaribleAuctionV1BidDataV1Dto
import com.rarible.protocol.dto.RaribleAuctionV1BidV1Dto
import com.rarible.protocol.dto.RaribleAuctionV1DataV1Dto
import com.rarible.protocol.dto.RaribleAuctionV1Dto
import com.rarible.protocol.dto.RaribleV2OrderDto
import com.rarible.protocol.dto.TransferDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import io.daonomic.rpc.domain.Word
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

fun randomAddressString() = EthConverter.convert(randomAddress())

fun randomEthAddress() = randomAddressString()
fun randomPolygonAddress() = UnionAddressConverter.convert(BlockchainDto.POLYGON, randomAddressString())

fun randomEthPartDto() = randomEthPartDto(randomAddress())
fun randomEthPartDto(account: Address) = PartDto(account, randomInt())

fun randomEthItemId() = ItemIdDto(BlockchainDto.ETHEREUM, randomEthAddress(), randomBigInt())
fun randomPolygonItemId() = randomEthItemId().copy(blockchain = BlockchainDto.POLYGON)

fun randomEthOwnershipId() = randomEthOwnershipId(randomEthItemId())
fun randomPolygonOwnershipId() = randomEthOwnershipId().copy(blockchain = BlockchainDto.POLYGON)
fun randomEthOwnershipId(itemId: ItemIdDto) = itemId.toOwnership(randomAddressString())

fun randomEthNftItemDto() = randomEthNftItemDto(randomEthItemId())
fun randomEthNftItemDto(itemId: ItemIdDto): NftItemDto {
    val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
    return NftItemDto(
        id = itemId.value,
        contract = Address.apply(contract),
        tokenId = tokenId,
        creators = listOf(randomEthPartDto(Address.apply(contract))),
        supply = randomBigInt(),
        lazySupply = randomBigInt(),
        royalties = listOf(randomEthPartDto()),
        lastUpdatedAt = nowMillis(),
        mintedAt = nowMillis().minusSeconds(1),
        owners = listOf(),
        pending = listOf(randomEthItemTransferDto()),
        deleted = false,
        meta = randomEthItemMeta()
    )
}

fun randomEthItemTransferDto(): ItemTransferDto {
    return ItemTransferDto(
        owner = randomAddress(),
        contract = randomAddress(),
        tokenId = randomBigInt(),
        value = randomBigInt(),
        date = nowMillis(),
        from = randomAddress()
    )
}

fun randomEthItemRoyaltyDto(): ItemRoyaltyDto {
    return ItemRoyaltyDto(
        owner = randomAddress(),
        contract = randomAddress(),
        tokenId = randomBigInt(),
        value = randomBigInt(),
        date = nowMillis(),
        royalties = listOf(randomEthPartDto())
    )
}

fun randomEthItemMeta(): NftItemMetaDto {
    return NftItemMetaDto(
        name = randomString(),
        description = randomString(),
        attributes = listOf(randomEthItemMetaAttribute()),
        image = randomEthItemMedia(),
        animation = randomEthItemMedia()
    )
}

fun randomEthItemMetaAttribute(): NftItemAttributeDto {
    return NftItemAttributeDto(
        key = randomString(),
        value = randomString()
    )
}

fun randomEthItemMedia(): NftMediaDto {
    val type = "ORIGINAL"
    return NftMediaDto(
        url = mapOf(Pair(type, randomString())),
        meta = mapOf(Pair(type, randomEthItemMediaMeta(type)))
    )
}

fun randomEthItemMediaMeta(type: String): NftMediaMetaDto {
    return NftMediaMetaDto(
        type = type,
        width = randomInt(400, 1200),
        height = randomInt(200, 800)
    )
}

fun randomEthOwnershipDto() = randomEthOwnershipDto(randomEthOwnershipId())
fun randomEthOwnershipDto(itemId: ItemIdDto) = randomEthOwnershipDto(
    itemId.toOwnership(randomAddressString())
)

fun randomEthOwnershipDto(ownershipId: OwnershipIdDto): NftOwnershipDto {
    return randomEthOwnershipDto(
        ownershipId.getItemId(),
        PartDto(Address.apply(ownershipId.owner.value), randomInt())
    )
}

fun randomEthOwnershipDto(itemId: ItemIdDto, creator: PartDto): NftOwnershipDto {
    val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
    val ownershipId = itemId.toOwnership(creator.account.toString())
    return NftOwnershipDto(
        id = ownershipId.value,
        contract = Address.apply(contract),
        tokenId = tokenId,
        owner = Address.apply(ownershipId.owner.value),
        creators = listOf(creator),
        value = randomBigInt(),
        lazyValue = randomBigInt(),
        date = nowMillis(),
        pending = listOf(randomEthItemTransferDto())
    )
}

fun randomEthAssetErc721() = randomEthAssetErc721(randomEthItemId())
fun randomEthAssetErc721(itemId: ItemIdDto): AssetDto {
    val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
    return AssetDto(
        assetType = Erc721AssetTypeDto(Address.apply(contract), tokenId),
        value = randomBigInt(),
        valueDecimal = randomBigInt().toBigDecimal()
    )
}

fun randomEthAssetErc20() = randomEthAssetErc20(randomAddress())
fun randomEthAssetErc20(address: Address) = AssetDto(
    assetType = Erc20AssetTypeDto(address),
    value = randomBigInt(),
    valueDecimal = randomBigDecimal()
)

fun randomEthCollectionAsset() = randomEthCollectionAsset(randomAddress())
fun randomEthCollectionAsset(address: Address) = AssetDto(
    assetType = CollectionAssetTypeDto(address),
    value = randomBigInt(),
    valueDecimal = randomBigDecimal()
)

fun randomEthAssetErc1155() = randomEthAssetErc1155(randomEthItemId())
fun randomEthAssetErc1155(itemId: ItemIdDto): AssetDto {
    val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
    return AssetDto(
        assetType = Erc1155AssetTypeDto(Address.apply(contract), tokenId),
        value = randomBigInt(),
        valueDecimal = randomBigInt().toBigDecimal()
    )
}

fun randomEthLegacySellOrderDto() =
    randomEthLegacyOrderDto(randomEthAssetErc721(), randomAddress(), randomEthAssetErc20())

fun randomEthLegacySellOrderDto(itemId: ItemIdDto) = randomEthLegacySellOrderDto(itemId, randomAddress())
fun randomEthLegacySellOrderDto(itemId: ItemIdDto, maker: Address) = randomEthLegacyOrderDto(
    randomEthAssetErc721(itemId),
    maker,
    randomEthAssetErc20()
)

fun randomEthLegacyBidOrderDto() =
    randomEthLegacyOrderDto(randomEthAssetErc20(), randomAddress(), randomEthAssetErc721())

fun randomEthLegacyBidOrderDto(itemId: ItemIdDto) = randomEthLegacyBidOrderDto(itemId, randomAddress())
fun randomEthLegacyBidOrderDto(itemId: ItemIdDto, maker: Address) = randomEthLegacyOrderDto(
    randomEthAssetErc20(),
    maker,
    randomEthAssetErc721(itemId)
)

fun randomEthLegacyOrderDto(make: AssetDto, maker: Address, take: AssetDto): LegacyOrderDto {
    val makeStockValue = randomBigDecimal()
    return LegacyOrderDto(
        status = OrderStatusDto.ACTIVE,
        maker = maker,
        taker = null,
        make = make,
        take = take,
        fill = randomBigInt(),
        fillValue = randomBigDecimal(),
        makeStock = makeStockValue.toBigInteger(),
        makeStockValue = makeStockValue,
        cancelled = false,
        salt = Word.apply(randomWord()),
        data = OrderDataLegacyDto(randomInt()),
        signature = randomBinary(),
        createdAt = nowMillis(),
        lastUpdateAt = nowMillis(),
        pending = listOf(randomEthOrderSideMatchDto()),
        hash = Word.apply(randomWord()),
        makeBalance = randomBigInt(),
        makePriceUsd = randomBigInt().toBigDecimal(),
        takePriceUsd = randomBigInt().toBigDecimal(),
        start = randomInt().toLong(),
        end = randomInt().toLong(),
        priceHistory = listOf()
    )
}

fun randomEthV2OrderDto() = randomEthV2OrderDto(randomEthAssetErc721(), randomAddress(), randomEthAssetErc20())
fun randomEthV2OrderDto(itemId: ItemIdDto) = randomEthV2OrderDto(itemId, randomAddress())
fun randomEthV2OrderDto(itemId: ItemIdDto, maker: Address) = randomEthV2OrderDto(
    randomEthAssetErc721(itemId),
    maker,
    randomEthAssetErc20()
)

fun randomEthV2OrderDto(make: AssetDto, maker: Address, take: AssetDto): RaribleV2OrderDto {
    val makeStockValue = randomBigDecimal()
    return RaribleV2OrderDto(
        status = OrderStatusDto.ACTIVE,
        maker = maker,
        taker = null,
        make = make,
        take = take,
        fill = randomBigInt(),
        fillValue = randomBigDecimal(),
        makeStock = makeStockValue.toBigInteger(),
        makeStockValue = makeStockValue,
        cancelled = false,
        salt = Word.apply(randomWord()),
        data = OrderRaribleV2DataV1Dto(listOf(randomEthPartDto()), listOf(randomEthPartDto())),
        signature = randomBinary(),
        createdAt = nowMillis(),
        lastUpdateAt = nowMillis(),
        pending = listOf(randomEthOrderSideMatchDto()),
        hash = Word.apply(randomWord()),
        makeBalance = randomBigInt(),
        makePriceUsd = randomBigInt().toBigDecimal(),
        takePriceUsd = randomBigInt().toBigDecimal(),
        start = randomInt().toLong(),
        end = randomInt().toLong(),
        priceHistory = listOf()
    )
}

fun randomEthOpenSeaV1OrderDto() =
    randomEthOpenSeaV1OrderDto(randomEthAssetErc721(), randomAddress(), randomEthAssetErc20())

fun randomEthOpenSeaV1OrderDto(itemId: ItemIdDto) = randomEthOpenSeaV1OrderDto(itemId, randomAddress())
fun randomEthOpenSeaV1OrderDto(itemId: ItemIdDto, maker: Address) = randomEthOpenSeaV1OrderDto(
    randomEthAssetErc721(itemId),
    maker,
    randomEthAssetErc20()
)

fun randomEthOpenSeaV1OrderDto(make: AssetDto, maker: Address, take: AssetDto): OpenSeaV1OrderDto {
    val makeStockValue = randomBigDecimal()
    return OpenSeaV1OrderDto(
        status = OrderStatusDto.ACTIVE,
        maker = maker,
        taker = null,
        make = make,
        take = take,
        fill = randomBigInt(),
        fillValue = randomBigDecimal(),
        makeStock = makeStockValue.toBigInteger(),
        makeStockValue = makeStockValue,
        cancelled = false,
        salt = Word.apply(randomWord()),
        data = randomEthOrderOpenSeaV1DataV1Dto(),
        signature = randomBinary(),
        createdAt = nowMillis(),
        lastUpdateAt = nowMillis(),
        pending = emptyList(),
        hash = Word.apply(randomWord()),
        makeBalance = randomBigInt(),
        makePriceUsd = randomBigInt().toBigDecimal(),
        takePriceUsd = randomBigInt().toBigDecimal(),
        start = randomInt().toLong(),
        end = randomInt().toLong(),
        priceHistory = listOf()
    )
}

fun randomEthCryptoPunksOrderDto() =
    randomEthCryptoPunksOrderDto(randomEthAssetErc721(), randomAddress(), randomEthAssetErc20())

fun randomEthCryptoPunksOrderDto(itemId: ItemIdDto) = randomEthCryptoPunksOrderDto(itemId, randomAddress())
fun randomEthCryptoPunksOrderDto(itemId: ItemIdDto, maker: Address) = randomEthCryptoPunksOrderDto(
    randomEthAssetErc721(itemId),
    maker,
    randomEthAssetErc20()
)

fun randomEthCryptoPunksOrderDto(make: AssetDto, maker: Address, take: AssetDto): CryptoPunkOrderDto {
    val makeStockValue = randomBigDecimal()
    return CryptoPunkOrderDto(
        status = OrderStatusDto.ACTIVE,
        maker = maker,
        taker = randomAddress(),
        make = make,
        take = take,
        fill = randomBigInt(),
        fillValue = randomBigDecimal(),
        makeStock = makeStockValue.toBigInteger(),
        makeStockValue = makeStockValue,
        cancelled = false,
        salt = Word.apply(randomWord()),
        data = OrderCryptoPunksDataDto(),
        signature = randomBinary(),
        createdAt = nowMillis(),
        lastUpdateAt = nowMillis(),
        pending = emptyList(),
        hash = Word.apply(randomWord()),
        makeBalance = randomBigInt(),
        makePriceUsd = randomBigInt().toBigDecimal(),
        takePriceUsd = randomBigInt().toBigDecimal(),
        start = randomInt().toLong(),
        end = randomInt().toLong(),
        priceHistory = listOf()
    )
}

fun randomEthOrderOpenSeaV1DataV1Dto(): OrderOpenSeaV1DataV1Dto {
    return OrderOpenSeaV1DataV1Dto(
        exchange = randomAddress(),
        makerRelayerFee = randomBigInt(),
        takerRelayerFee = randomBigInt(),
        makerProtocolFee = randomBigInt(),
        takerProtocolFee = randomBigInt(),
        feeRecipient = randomAddress(),
        feeMethod = OrderOpenSeaV1DataV1Dto.FeeMethod.PROTOCOL_FEE,
        side = OrderOpenSeaV1DataV1Dto.Side.SELL,
        saleKind = OrderOpenSeaV1DataV1Dto.SaleKind.DUTCH_AUCTION,
        howToCall = OrderOpenSeaV1DataV1Dto.HowToCall.CALL,
        callData = randomBinary(),
        replacementPattern = randomBinary(),
        staticTarget = randomAddress(),
        staticExtraData = randomBinary(),
        extra = randomBigInt()
    )
}

fun randomEthOrderSideMatchDto(): OrderSideMatchDto {
    return OrderSideMatchDto(
        date = nowMillis(),
        hash = Word.apply(randomWord()),
        make = randomEthAssetErc721(),
        take = randomEthAssetErc20(),
        maker = randomAddress(),
        side = OrderSideDto.values()[randomInt(OrderSideDto.values().size)],
        fill = randomBigInt(),
        taker = randomAddress(),
        counterHash = Word.apply(randomWord()),
        makeUsd = randomBigDecimal(),
        takeUsd = randomBigDecimal(),
        makePriceUsd = randomBigDecimal(),
        takePriceUsd = randomBigDecimal()
    )
}

fun randomEthOrderCancelDto(): OrderCancelDto {
    return OrderCancelDto(
        date = nowMillis(),
        hash = Word.apply(randomWord()),
        make = randomEthAssetErc721(),
        take = randomEthAssetErc20(),
        maker = randomAddress(),
        owner = randomAddress()
    )
}

fun randomEthOnChainOrderDto(): OnChainOrderDto {
    return OnChainOrderDto(
        date = nowMillis(),
        hash = Word.apply(randomWord()),
        make = randomEthAssetErc721(),
        take = randomEthAssetErc20(),
        maker = randomAddress()
    )
}

fun randomEthCollectionId() = CollectionIdDto(BlockchainDto.ETHEREUM, randomEthAddress())

fun randomEthCollectionDto() = randomEthCollectionDto(randomAddress())
fun randomEthCollectionDto(id: Address): NftCollectionDto {
    return NftCollectionDto(
        id = id,
        name = randomString(),
        symbol = randomString(2),
        type = NftCollectionDto.Type.ERC1155,
        owner = randomAddress(),
        features = listOf(NftCollectionDto.Features.values()[randomInt(NftCollectionDto.Features.values().size)]),
        supportsLazyMint = true
    )
}

fun randomEthAuctionDto() = randomEthAuctionDto(randomEthItemId())
fun randomEthAuctionDto(itemId: ItemIdDto): AuctionDto {
    return RaribleAuctionV1Dto(
        contract = randomAddress(),
        seller = randomAddress(),
        sell = randomEthAssetErc721(itemId),
        buy = Erc20AssetTypeDto(randomAddress()),
        endTime = Instant.MAX,
        minimalStep = BigDecimal.ONE,
        minimalPrice = BigDecimal.ONE,
        createdAt = Instant.now(),
        lastUpdateAt = Instant.now(),
        buyPrice = BigDecimal.TEN,
        pending = listOf(AuctionHistoryDto(Word.apply(randomWord()))),
        status = AuctionStatusDto.ACTIVE,
        ongoing = true,
        buyPriceUsd = BigDecimal.TEN,
        hash = Word.apply(randomWord()),
        auctionId = BigInteger.ONE,
        lastBid = RaribleAuctionV1BidV1Dto(
            buyer = randomAddress(),
            amount = BigDecimal.ONE,
            data = RaribleAuctionV1BidDataV1Dto(
                originFees = listOf(PartDto(randomAddress(), 100)),
                payouts = listOf(PartDto(randomAddress(), 100))
            ),
            date = Instant.now(),
            status = AuctionBidDto.Status.ACTIVE
        ),
        data = RaribleAuctionV1DataV1Dto(
            originFees = listOf(PartDto(randomAddress(), 100)),
            payouts = listOf(PartDto(randomAddress(), 100)),
            startTime = Instant.now(),
            duration = BigInteger.TEN,
            buyOutPrice = BigDecimal.TEN
        )
    )
}

fun randomEthAuctionBidsDto(): AuctionBidsPaginationDto {
    return AuctionBidsPaginationDto(
        bids = listOf(
            RaribleAuctionV1BidV1Dto(
                buyer = randomAddress(),
                amount = BigDecimal.TEN,
                data = RaribleAuctionV1BidDataV1Dto(
                    originFees = listOf(PartDto(randomAddress(), 100)),
                    payouts = listOf(PartDto(randomAddress(), 100))
                ),
                date = Instant.now(),
                status = AuctionBidDto.Status.ACTIVE
            ),
            RaribleAuctionV1BidV1Dto(
                buyer = randomAddress(),
                amount = BigDecimal.ONE,
                data = RaribleAuctionV1BidDataV1Dto(
                    originFees = listOf(PartDto(randomAddress(), 100)),
                    payouts = listOf(PartDto(randomAddress(), 100))
                ),
                date = Instant.now().minusSeconds(10),
                status = AuctionBidDto.Status.HISTORICAL
            )
        ),
        continuation = null
    )
}

fun randomEthOrderActivityMatch(): OrderActivityMatchDto {
    return OrderActivityMatchDto(
        id = randomString(),
        date = nowMillis(),
        source = OrderActivityDto.Source.RARIBLE,
        left = randomEthOrderActivityMatchSide(),
        right = randomEthOrderActivityMatchSide(),
        price = randomBigDecimal(),
        priceUsd = randomBigDecimal(),
        transactionHash = Word.apply(randomWord()),
        blockHash = Word.apply(randomWord()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        type = OrderActivityMatchDto.Type.SELL,
        reverted = false
    )
}

fun randomEthOrderBidActivity(): OrderActivityBidDto {
    return OrderActivityBidDto(
        id = randomString(),
        date = nowMillis(),
        source = OrderActivityDto.Source.RARIBLE,
        hash = Word.apply(randomWord()),
        maker = randomAddress(),
        make = AssetDto(Erc20AssetTypeDto(randomAddress()), randomBigInt(), randomBigDecimal()),
        take = AssetDto(Erc20AssetTypeDto(randomAddress()), randomBigInt(), randomBigDecimal()),
        price = randomBigDecimal(),
        priceUsd = randomBigDecimal(),
        reverted = false
    )
}

fun randomEthAuctionOpenActivity(): AuctionActivityOpenDto {
    return AuctionActivityOpenDto(
        id = randomString(),
        date = nowMillis(),
        source = AuctionActivityDto.Source.RARIBLE,
        auction = randomEthAuctionDto(),
        transactionHash = Word.apply(randomWord()),
        blockHash = Word.apply(randomWord()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        reverted = false
    )
}

fun randomEthAuctionCancelActivity(): AuctionActivityCancelDto {
    return AuctionActivityCancelDto(
        id = randomString(),
        date = nowMillis(),
        source = AuctionActivityDto.Source.RARIBLE,
        auction = randomEthAuctionDto(),
        transactionHash = Word.apply(randomWord()),
        blockHash = Word.apply(randomWord()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        reverted = false
    )
}

fun randomEthAuctionFinishActivity(): AuctionActivityFinishDto {
    return AuctionActivityFinishDto(
        id = randomString(),
        date = nowMillis(),
        source = AuctionActivityDto.Source.RARIBLE,
        auction = randomEthAuctionDto(),
        transactionHash = Word.apply(randomWord()),
        blockHash = Word.apply(randomWord()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        reverted = false
    )
}

fun randomEthAuctionBidActivity(): AuctionActivityBidDto {
    return AuctionActivityBidDto(
        id = randomString(),
        date = nowMillis(),
        source = AuctionActivityDto.Source.RARIBLE,
        auction = randomEthAuctionDto(),
        transactionHash = Word.apply(randomWord()),
        blockHash = Word.apply(randomWord()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        bid = RaribleAuctionV1BidV1Dto(
            buyer = randomAddress(),
            amount = randomBigDecimal(),
            data = RaribleAuctionV1BidDataV1Dto(listOf(randomEthPartDto()), listOf(randomEthPartDto())),
            date = Instant.now(),
            status = AuctionBidDto.Status.ACTIVE
        ),
        reverted = false
    )
}

fun randomEthAuctionStartActivity(): AuctionActivityStartDto {
    return AuctionActivityStartDto(
        id = randomString(),
        date = nowMillis(),
        source = AuctionActivityDto.Source.RARIBLE,
        auction = randomEthAuctionDto(),
        reverted = false
    )
}

fun randomEthAuctionEndActivity(): AuctionActivityEndDto {
    return AuctionActivityEndDto(
        id = randomString(),
        date = nowMillis(),
        source = AuctionActivityDto.Source.RARIBLE,
        auction = randomEthAuctionDto(),
        reverted = false
    )
}

fun randomEthOrderListActivity(): OrderActivityListDto {
    return OrderActivityListDto(
        id = randomString(),
        date = nowMillis(),
        source = OrderActivityDto.Source.OPEN_SEA,
        hash = Word.apply(randomWord()),
        maker = randomAddress(),
        make = AssetDto(Erc20AssetTypeDto(randomAddress()), randomBigInt(), randomBigDecimal()),
        take = AssetDto(Erc20AssetTypeDto(randomAddress()), randomBigInt(), randomBigDecimal()),
        price = randomBigDecimal(),
        priceUsd = randomBigDecimal(),
        reverted = false
    )
}

fun randomEthOrderActivityCancelBid(): OrderActivityCancelBidDto {
    return OrderActivityCancelBidDto(
        id = randomString(),
        date = nowMillis(),
        source = OrderActivityDto.Source.RARIBLE,
        transactionHash = Word.apply(randomWord()),
        blockHash = Word.apply(randomWord()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        maker = randomAddress(),
        hash = Word.apply(randomWord()),
        make = randomEthAssetErc20().assetType,
        take = randomEthAssetErc721().assetType,
        reverted = false
    )
}

fun randomEthOrderActivityCancelList(): OrderActivityCancelListDto {
    return OrderActivityCancelListDto(
        id = randomString(),
        date = nowMillis(),
        source = OrderActivityDto.Source.RARIBLE,
        transactionHash = Word.apply(randomWord()),
        blockHash = Word.apply(randomWord()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        maker = randomAddress(),
        hash = Word.apply(randomWord()),
        make = randomEthAssetErc20().assetType,
        take = randomEthAssetErc721().assetType,
        reverted = false
    )
}

fun randomEthItemMintActivity(): MintDto {
    return MintDto(
        id = randomString(),
        date = nowMillis(),
        owner = randomAddress(),
        contract = randomAddress(),
        tokenId = randomBigInt(),
        value = randomBigInt(),
        transactionHash = Word.apply(randomWord()),
        blockHash = Word.apply(randomWord()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        reverted = false
    )
}

fun randomEthItemBurnActivity(): BurnDto {
    return BurnDto(
        id = randomString(),
        date = nowMillis(),
        owner = randomAddress(),
        contract = randomAddress(),
        tokenId = randomBigInt(),
        value = randomBigInt(),
        transactionHash = Word.apply(randomWord()),
        blockHash = Word.apply(randomWord()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        reverted = false
    )
}

fun randomEthItemTransferActivity(): TransferDto {
    return TransferDto(
        id = randomString(),
        date = nowMillis(),
        owner = randomAddress(),
        contract = randomAddress(),
        from = randomAddress(),
        tokenId = randomBigInt(),
        value = randomBigInt(),
        transactionHash = Word.apply(randomWord()),
        blockHash = Word.apply(randomWord()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        reverted = false,
        purchase = false
    )
}

fun randomEthOrderActivityMatchSide(): OrderActivityMatchSideDto {
    return OrderActivityMatchSideDto(
        maker = randomAddress(),
        hash = Word.apply(randomWord()),
        asset = randomEthAssetErc20(),
        type = OrderActivityMatchSideDto.Type.values()[randomInt(OrderActivityMatchSideDto.Type.values().size)]
    )
}
