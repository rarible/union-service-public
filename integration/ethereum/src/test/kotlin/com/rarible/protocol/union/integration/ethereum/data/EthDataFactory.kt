package com.rarible.protocol.union.integration.ethereum.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBinary
import com.rarible.core.test.data.randomBoolean
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
import com.rarible.protocol.dto.EthCollectionMetaDto
import com.rarible.protocol.dto.EthMetaStatusDto
import com.rarible.protocol.dto.EventTimeMarkDto
import com.rarible.protocol.dto.EventTimeMarksDto
import com.rarible.protocol.dto.ImageContentDto
import com.rarible.protocol.dto.ItemRoyaltyDto
import com.rarible.protocol.dto.ItemTransferDto
import com.rarible.protocol.dto.LooksRareOrderDto
import com.rarible.protocol.dto.MetaContentDto
import com.rarible.protocol.dto.MintDto
import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.dto.NftItemAttributeDto
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemMetaDto
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
import com.rarible.protocol.dto.OrderBasicSeaportDataV1Dto
import com.rarible.protocol.dto.OrderCancelDto
import com.rarible.protocol.dto.OrderCryptoPunksDataDto
import com.rarible.protocol.dto.OrderLooksRareDataV1Dto
import com.rarible.protocol.dto.OrderOpenSeaV1DataV1Dto
import com.rarible.protocol.dto.OrderRaribleV2DataDto
import com.rarible.protocol.dto.OrderRaribleV2DataV1Dto
import com.rarible.protocol.dto.OrderRaribleV2DataV3BuyDto
import com.rarible.protocol.dto.OrderRaribleV2DataV3SellDto
import com.rarible.protocol.dto.OrderSideDto
import com.rarible.protocol.dto.OrderSideMatchDto
import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.dto.OrderX2Y2DataDto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.dto.RaribleAuctionV1BidDataV1Dto
import com.rarible.protocol.dto.RaribleAuctionV1BidV1Dto
import com.rarible.protocol.dto.RaribleAuctionV1DataV1Dto
import com.rarible.protocol.dto.RaribleAuctionV1Dto
import com.rarible.protocol.dto.RaribleV2OrderDto
import com.rarible.protocol.dto.SeaportConsiderationDto
import com.rarible.protocol.dto.SeaportItemTypeDto
import com.rarible.protocol.dto.SeaportOfferDto
import com.rarible.protocol.dto.SeaportOrderTypeDto
import com.rarible.protocol.dto.SeaportV1OrderDto
import com.rarible.protocol.dto.TransferDto
import com.rarible.protocol.dto.X2Y2OrderDto
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
        deleted = false
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
        tags = emptyList(),
        genres = emptyList(),
        content = randomMetaContentDtoList(),
        status = EthMetaStatusDto.OK
    )
}

fun randomEthItemMetaAttribute(): NftItemAttributeDto {
    return NftItemAttributeDto(
        key = randomString(),
        value = randomString()
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
        lastUpdatedAt = nowMillis().plusSeconds(1),
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

fun randomEthCollectionAsset(
    address: Address = randomAddress()
) = AssetDto(
    assetType = CollectionAssetTypeDto(address),
    value = randomBigInt(),
    valueDecimal = randomBigDecimal()
)

fun randomEthAssetErc1155(
    itemId: ItemIdDto = randomEthItemId()
): AssetDto {
    val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
    return AssetDto(
        assetType = Erc1155AssetTypeDto(Address.apply(contract), tokenId),
        value = randomBigInt(),
        valueDecimal = randomBigInt().toBigDecimal()
    )
}

fun randomEthSellOrderDto(
    itemId: ItemIdDto = randomEthItemId(),
    maker: Address = randomAddress(),
    data: OrderRaribleV2DataDto = OrderRaribleV2DataV1Dto(emptyList(), emptyList())
) = randomEthLegacyOrderDto(
    randomEthAssetErc721(itemId),
    maker,
    randomEthAssetErc20(),
    data
)

fun randomEthBidOrderDto(
    itemId: ItemIdDto = randomEthItemId(),
    maker: Address = randomAddress(),
    data: OrderRaribleV2DataDto = OrderRaribleV2DataV1Dto(emptyList(), emptyList())
) = randomEthLegacyOrderDto(
    randomEthAssetErc20(),
    maker,
    randomEthAssetErc721(itemId),
    data
)

private fun randomEthLegacyOrderDto(
    make: AssetDto,
    maker: Address,
    take: AssetDto,
    data: OrderRaribleV2DataDto
): RaribleV2OrderDto {
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
        data = data,
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
        priceHistory = listOf(),
        optionalRoyalties = randomBoolean()
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
        priceHistory = listOf(),
        optionalRoyalties = randomBoolean()
    )
}

fun randomEthOrderDataRaribleV2DataV3SellDto() = OrderRaribleV2DataV3SellDto(
    payout = randomEthPartDto(),
    originFeeFirst = randomEthPartDto(),
    originFeeSecond = randomEthPartDto(),
    maxFeesBasePoint = randomInt(),
    marketplaceMarker = Word.apply(randomWord())
)

fun randomEthOrderDataRaribleV2DataV3BuyDto() = OrderRaribleV2DataV3BuyDto(
    payout = randomEthPartDto(),
    originFeeFirst = randomEthPartDto(),
    originFeeSecond = randomEthPartDto(),
    marketplaceMarker = Word.apply(randomWord())
)

fun randomEthOpenSeaV1OrderDto() =
    randomEthOpenSeaV1OrderDto(randomEthAssetErc721(), randomAddress(), randomEthAssetErc20())

fun randomEthSeaportV1OrderDto() =
    randomEthSeaportV1OrderDto(randomEthAssetErc721(), randomAddress(), randomEthAssetErc20())

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

fun randomEthSeaportV1OrderDto(make: AssetDto, maker: Address, take: AssetDto): SeaportV1OrderDto {
    val makeStockValue = randomBigDecimal()
    return SeaportV1OrderDto(
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
        data = randomEthOrderBasicSeaportDataV1Dto(),
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

fun randomEthOrderBasicSeaportDataV1Dto(): OrderBasicSeaportDataV1Dto {
    val counter = randomLong()
    return OrderBasicSeaportDataV1Dto(
        protocol = randomAddress(),
        orderType = SeaportOrderTypeDto.values().random(),
        offer = listOf(randomEthSeaportOffer(), randomEthSeaportOffer()),
        consideration = listOf(randomEthSeaportConsideration(), randomEthSeaportConsideration()),
        zone = randomAddress(),
        zoneHash = Word.apply(randomWord()),
        conduitKey = Word.apply(randomWord()),
        counter = counter,
        nonce = counter.toBigInteger()
    )
}

fun randomEthSeaportOffer(): SeaportOfferDto {
    return SeaportOfferDto(
        itemType = SeaportItemTypeDto.values().random(),
        token = randomAddress(),
        identifierOrCriteria = randomBigInt(),
        startAmount = randomBigInt(),
        endAmount = randomBigInt()
    )
}

fun randomEthSeaportConsideration(): SeaportConsiderationDto {
    return SeaportConsiderationDto(
        itemType = SeaportItemTypeDto.values().random(),
        token = randomAddress(),
        identifierOrCriteria = randomBigInt(),
        startAmount = randomBigInt(),
        endAmount = randomBigInt(),
        recipient = randomAddress()
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
        supportsLazyMint = true,
        status = NftCollectionDto.Status.CONFIRMED,
        // meta = randomEthCollectionMetaDto(),
        minters = emptyList()
    )
}

fun randomEthCollectionMetaDto(): EthCollectionMetaDto {
    return EthCollectionMetaDto(
        name = randomString(),
        description = randomString(),
        language = randomString(),
        genres = listOf(randomString(), randomString()),
        tags = listOf(randomString(), randomString()),
        createdAt = nowMillis(),
        rights = randomString(),
        rightsUri = randomString(),
        externalUri = randomString(),
        originalMetaUri = randomString(),
        feeRecipient = randomAddress(),
        sellerFeeBasisPoints = randomInt(),
        content = randomMetaContentDtoList()
    )
}

fun randomMetaContentDtoList(): List<MetaContentDto> {
    return listOf(
        ImageContentDto(
            fileName = randomString(),
            url = randomString(),
            representation = MetaContentDto.Representation.ORIGINAL,
            mimeType = randomString(),
            size = randomLong(),
            width = randomInt(400, 1200),
            height = randomInt(200, 800)
        )
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
        createdAt = nowMillis(),
        lastUpdateAt = nowMillis(),
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
            date = nowMillis(),
            status = AuctionBidDto.Status.ACTIVE
        ),
        data = RaribleAuctionV1DataV1Dto(
            originFees = listOf(PartDto(randomAddress(), 100)),
            payouts = listOf(PartDto(randomAddress(), 100)),
            startTime = nowMillis(),
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
                date = nowMillis(),
                status = AuctionBidDto.Status.ACTIVE
            ),
            RaribleAuctionV1BidV1Dto(
                buyer = randomAddress(),
                amount = BigDecimal.ONE,
                data = RaribleAuctionV1BidDataV1Dto(
                    originFees = listOf(PartDto(randomAddress(), 100)),
                    payouts = listOf(PartDto(randomAddress(), 100))
                ),
                date = nowMillis().minusSeconds(10),
                status = AuctionBidDto.Status.HISTORICAL
            )
        ),
        continuation = null
    )
}

fun randomEthOrderActivityMatch(): OrderActivityMatchDto {
    return OrderActivityMatchDto(
        id = randomString().lowercase(),
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
        reverted = false,
        lastUpdatedAt = nowMillis(),
        marketplaceMarker = Word.apply(randomWord()),
        counterMarketplaceMarker = Word.apply(randomWord())
    )
}

fun randomEthOrderBidActivity(): OrderActivityBidDto {
    return OrderActivityBidDto(
        id = randomString().lowercase(),
        date = nowMillis(),
        source = OrderActivityDto.Source.RARIBLE,
        hash = Word.apply(randomWord()),
        maker = randomAddress(),
        make = AssetDto(Erc20AssetTypeDto(randomAddress()), randomBigInt(), randomBigDecimal()),
        take = AssetDto(Erc20AssetTypeDto(randomAddress()), randomBigInt(), randomBigDecimal()),
        price = randomBigDecimal(),
        priceUsd = randomBigDecimal(),
        reverted = false,
        lastUpdatedAt = nowMillis()
    )
}

fun randomEthAuctionOpenActivity(): AuctionActivityOpenDto {
    return AuctionActivityOpenDto(
        id = randomString().lowercase(),
        date = nowMillis(),
        source = AuctionActivityDto.Source.RARIBLE,
        auction = randomEthAuctionDto(),
        transactionHash = Word.apply(randomWord()),
        blockHash = Word.apply(randomWord()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        reverted = false,
        lastUpdatedAt = nowMillis()
    )
}

fun randomEthAuctionCancelActivity(): AuctionActivityCancelDto {
    return AuctionActivityCancelDto(
        id = randomString().lowercase(),
        date = nowMillis(),
        source = AuctionActivityDto.Source.RARIBLE,
        auction = randomEthAuctionDto(),
        transactionHash = Word.apply(randomWord()),
        blockHash = Word.apply(randomWord()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        reverted = false,
        lastUpdatedAt = nowMillis()
    )
}

fun randomEthAuctionFinishActivity(): AuctionActivityFinishDto {
    return AuctionActivityFinishDto(
        id = randomString().lowercase(),
        date = nowMillis(),
        source = AuctionActivityDto.Source.RARIBLE,
        auction = randomEthAuctionDto(),
        transactionHash = Word.apply(randomWord()),
        blockHash = Word.apply(randomWord()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        reverted = false,
        lastUpdatedAt = nowMillis()
    )
}

fun randomEthAuctionBidActivity(): AuctionActivityBidDto {
    return AuctionActivityBidDto(
        id = randomString().lowercase(),
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
            date = nowMillis(),
            status = AuctionBidDto.Status.ACTIVE
        ),
        reverted = false,
        lastUpdatedAt = nowMillis()
    )
}

fun randomEthAuctionStartActivity(): AuctionActivityStartDto {
    return AuctionActivityStartDto(
        id = randomString().lowercase(),
        date = nowMillis(),
        source = AuctionActivityDto.Source.RARIBLE,
        auction = randomEthAuctionDto(),
        reverted = false,
        lastUpdatedAt = nowMillis()
    )
}

fun randomEthAuctionEndActivity(): AuctionActivityEndDto {
    return AuctionActivityEndDto(
        id = randomString().lowercase(),
        date = nowMillis(),
        source = AuctionActivityDto.Source.RARIBLE,
        auction = randomEthAuctionDto(),
        reverted = false,
        lastUpdatedAt = nowMillis()
    )
}

fun randomEthOrderListActivity(): OrderActivityListDto {
    return OrderActivityListDto(
        id = randomString().lowercase(),
        date = nowMillis(),
        source = OrderActivityDto.Source.OPEN_SEA,
        hash = Word.apply(randomWord()),
        maker = randomAddress(),
        make = AssetDto(Erc20AssetTypeDto(randomAddress()), randomBigInt(), randomBigDecimal()),
        take = AssetDto(Erc20AssetTypeDto(randomAddress()), randomBigInt(), randomBigDecimal()),
        price = randomBigDecimal(),
        priceUsd = randomBigDecimal(),
        reverted = false,
        lastUpdatedAt = nowMillis()
    )
}

fun randomEthOrderActivityCancelBid(): OrderActivityCancelBidDto {
    return OrderActivityCancelBidDto(
        id = randomString().lowercase(),
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
        reverted = false,
        lastUpdatedAt = nowMillis()
    )
}

fun randomEthOrderActivityCancelList(): OrderActivityCancelListDto {
    return OrderActivityCancelListDto(
        id = randomString().lowercase(),
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
        reverted = false,
        lastUpdatedAt = nowMillis()
    )
}

fun randomEthItemMintActivity(): MintDto {
    return MintDto(
        id = randomString().lowercase(),
        date = nowMillis(),
        owner = randomAddress(),
        contract = randomAddress(),
        tokenId = randomBigInt(),
        value = randomBigInt(),
        transactionHash = Word.apply(randomWord()),
        blockHash = Word.apply(randomWord()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        reverted = false,
        lastUpdatedAt = nowMillis(),
        mintPrice = randomBigDecimal()
    )
}

fun randomEthItemBurnActivity(): BurnDto {
    return BurnDto(
        id = randomString().lowercase(),
        date = nowMillis(),
        owner = randomAddress(),
        contract = randomAddress(),
        tokenId = randomBigInt(),
        value = randomBigInt(),
        transactionHash = Word.apply(randomWord()),
        blockHash = Word.apply(randomWord()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        reverted = false,
        lastUpdatedAt = nowMillis()
    )
}

fun randomEthItemTransferActivity(): TransferDto {
    return TransferDto(
        id = randomString().lowercase(),
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
        purchase = false,
        lastUpdatedAt = nowMillis()
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

fun randomEthEventTimeMarks(): EventTimeMarksDto {
    return EventTimeMarksDto(
        source = randomString(),
        marks = listOf(
            EventTimeMarkDto(
                randomString(),
                nowMillis().minusSeconds(randomLong(1000))
            )
        )
    )
}

fun randomEthX2Y2OrderDto(): X2Y2OrderDto {
    val makeStockValue = randomBigDecimal()
    return X2Y2OrderDto(
        status = OrderStatusDto.ACTIVE,
        maker = randomAddress(),
        taker = randomAddress(),
        make = randomEthAssetErc721(),
        take = randomEthAssetErc20(),
        fill = randomBigInt(),
        fillValue = randomBigDecimal(),
        makeStock = makeStockValue.toBigInteger(),
        makeStockValue = makeStockValue,
        cancelled = false,
        salt = Word.apply(randomWord()),
        data = OrderX2Y2DataDto(
            itemHash = Word.apply(randomWord()),
            orderId = randomBigInt(),
            isBundle = false,
            isCollectionOffer = false,
            side = randomInt(),
        ),
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

fun randomEthLooksRareOrderDto(): LooksRareOrderDto {
    val makeStockValue = randomBigDecimal()
    return LooksRareOrderDto(
        status = OrderStatusDto.ACTIVE,
        maker = randomAddress(),
        taker = randomAddress(),
        make = randomEthAssetErc721(),
        take = randomEthAssetErc20(),
        fill = randomBigInt(),
        fillValue = randomBigDecimal(),
        makeStock = makeStockValue.toBigInteger(),
        makeStockValue = makeStockValue,
        cancelled = false,
        salt = Word.apply(randomWord()),
        data = OrderLooksRareDataV1Dto(
            minPercentageToAsk = randomInt(),
            nonce = randomLong(),
            params = randomBinary(),
            strategy = randomAddress()
        ),
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
