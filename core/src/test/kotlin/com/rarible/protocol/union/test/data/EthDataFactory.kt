package com.rarible.protocol.union.test.data

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
import com.rarible.protocol.dto.BurnDto
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
import com.rarible.protocol.dto.OrderPriceHistoryRecordDto
import com.rarible.protocol.dto.OrderRaribleV2DataV1Dto
import com.rarible.protocol.dto.OrderSideDto
import com.rarible.protocol.dto.OrderSideMatchDto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.dto.RaribleV2OrderDto
import com.rarible.protocol.dto.TransferDto
import com.rarible.protocol.union.core.ethereum.converter.EthConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.parser.ItemIdParser
import io.daonomic.rpc.domain.Word
import scalether.domain.Address

fun randomAddressString() = EthConverter.convert(randomAddress())

fun randomEthAddress() = UnionAddress(BlockchainDto.ETHEREUM, randomAddressString())
fun randomPolygonAddress() = UnionAddress(BlockchainDto.POLYGON, randomAddressString())

fun randomEthPartDto() = randomEthPartDto(randomAddress())
fun randomEthPartDto(account: Address) = PartDto(account, randomInt())

fun randomEthItemId() = ItemIdDto(BlockchainDto.ETHEREUM, randomEthAddress(), randomBigInt())
fun randomPolygonItemId() = randomEthItemId().copy(blockchain = BlockchainDto.POLYGON)

fun randomEthOwnershipId() = randomEthOwnershipId(randomEthItemId())
fun randomPolygonOwnershipId() = randomEthOwnershipId().copy(blockchain = BlockchainDto.POLYGON)
fun randomEthOwnershipId(itemId: ItemIdDto) = randomEthOwnershipId(itemId, randomAddressString())
fun randomEthOwnershipId(itemId: ItemIdDto, owner: String): OwnershipIdDto {
    return OwnershipIdDto(
        token = itemId.token,
        tokenId = itemId.tokenId,
        owner = UnionAddress(BlockchainDto.ETHEREUM, owner),
        blockchain = BlockchainDto.ETHEREUM
    )
}

fun randomEthNftItemDto() = randomEthNftItemDto(randomEthItemId())
fun randomEthNftItemDto(itemId: ItemIdDto): NftItemDto {
    return NftItemDto(
        id = itemId.value,
        contract = Address.apply(itemId.token.value),
        tokenId = itemId.tokenId,
        creators = listOf(randomEthPartDto(Address.apply(itemId.token.value))),
        supply = randomBigInt(),
        lazySupply = randomBigInt(),
        royalties = listOf(randomEthPartDto()),
        date = nowMillis(),
        owners = listOf(randomAddress()),
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
    val type = randomString()
    return NftMediaDto(
        url = mapOf(Pair(randomString(), randomString())),
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
    OwnershipIdDto(
        itemId.blockchain,
        itemId.token,
        itemId.tokenId,
        UnionAddress(BlockchainDto.ETHEREUM, randomAddressString())
    )
)

fun randomEthOwnershipDto(ownershipId: OwnershipIdDto) = randomEthOwnershipDto(
    ItemIdParser.parseShort("${ownershipId.token.value}:${ownershipId.tokenId}", BlockchainDto.ETHEREUM),
    PartDto(Address.apply(ownershipId.owner.value), randomInt())
)

fun randomEthOwnershipDto(itemId: ItemIdDto, creator: PartDto): NftOwnershipDto {
    val ownershipId = randomEthOwnershipId(itemId, creator.account.toString())
    return NftOwnershipDto(
        id = ownershipId.value,
        contract = Address.apply(ownershipId.token.value),
        tokenId = ownershipId.tokenId,
        owner = Address.apply(ownershipId.owner.value),
        creators = listOf(creator),
        value = randomBigInt(),
        lazyValue = randomBigInt(),
        date = nowMillis(),
        pending = listOf(randomEthItemTransferDto())
    )
}

fun randomEthAssetErc721() = randomEthAssetErc721(randomEthItemId())
fun randomEthAssetErc721(itemId: ItemIdDto) = AssetDto(
    assetType = Erc721AssetTypeDto(Address.apply(itemId.token.value), itemId.tokenId),
    value = randomBigInt(),
    valueDecimal = randomBigDecimal()
)

fun randomEthAssetErc20() = randomEthAssetErc20(randomAddress())
fun randomEthAssetErc20(address: Address) = AssetDto(
    assetType = Erc20AssetTypeDto(address),
    value = randomBigInt(),
    valueDecimal = randomBigDecimal()
)

fun randomEthAssetErc1155(itemId: ItemIdDto) = AssetDto(
    assetType = Erc1155AssetTypeDto(Address.apply(itemId.token.value), itemId.tokenId),
    value = randomBigInt(),
    valueDecimal = randomBigDecimal()
)

fun randomEthLegacyOrderDto() = randomEthLegacyOrderDto(randomEthAssetErc721(), randomAddress(), randomEthAssetErc20())
fun randomEthLegacyOrderDto(itemId: ItemIdDto) = randomEthLegacyOrderDto(itemId, randomAddress())
fun randomEthLegacyOrderDto(itemId: ItemIdDto, maker: Address) = randomEthLegacyOrderDto(
    randomEthAssetErc721(itemId),
    maker,
    randomEthAssetErc20()
)

fun randomEthLegacyOrderDto(make: AssetDto, maker: Address, take: AssetDto): LegacyOrderDto {
    return LegacyOrderDto(
        maker = maker,
        taker = randomAddress(),
        make = make,
        take = take,
        fill = randomBigInt(),
        fillValue = randomBigDecimal(),
        makeStock = randomBigInt(),
        makeStockValue = randomBigDecimal(),
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
        priceHistory = listOf(randomEthOrderPriceHistoryRecordDto())
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
    return RaribleV2OrderDto(
        maker = maker,
        taker = randomAddress(),
        make = make,
        take = take,
        fill = randomBigInt(),
        fillValue = randomBigDecimal(),
        makeStock = randomBigInt(),
        makeStockValue = randomBigDecimal(),
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
        priceHistory = listOf(randomEthOrderPriceHistoryRecordDto())
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
    return OpenSeaV1OrderDto(
        maker = maker,
        taker = randomAddress(),
        make = make,
        take = take,
        fill = randomBigInt(),
        fillValue = randomBigDecimal(),
        makeStock = randomBigInt(),
        makeStockValue = randomBigDecimal(),
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
        priceHistory = listOf(randomEthOrderPriceHistoryRecordDto())
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
    return CryptoPunkOrderDto(
        maker = maker,
        taker = randomAddress(),
        make = make,
        take = take,
        fill = randomBigInt(),
        fillValue = randomBigDecimal(),
        makeStock = randomBigInt(),
        makeStockValue = randomBigDecimal(),
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
        priceHistory = listOf(randomEthOrderPriceHistoryRecordDto())
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

fun randomEthOrderPriceHistoryRecordDto(): OrderPriceHistoryRecordDto {
    return OrderPriceHistoryRecordDto(
        date = nowMillis(),
        makeValue = randomBigDecimal(),
        takeValue = randomBigDecimal()
    )
}

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
        logIndex = randomInt()
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
        priceUsd = randomBigDecimal()
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
        priceUsd = randomBigDecimal()
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
        take = randomEthAssetErc721().assetType
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
        take = randomEthAssetErc721().assetType
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
        logIndex = randomInt()
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
        logIndex = randomInt()
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
        logIndex = randomInt()
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