package com.rarible.protocol.union.test.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.*
import com.rarible.protocol.dto.*
import com.rarible.protocol.union.core.ethereum.converter.EthConverter
import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.dto.EthItemIdDto
import com.rarible.protocol.union.dto.EthOrderIdDto
import com.rarible.protocol.union.dto.EthOwnershipIdDto
import com.rarible.protocol.union.dto.ethereum.EthAddress
import com.rarible.protocol.union.dto.ethereum.EthItemIdProvider
import com.rarible.protocol.union.dto.ethereum.EthOwnershipIdProvider
import io.daonomic.rpc.domain.Word
import scalether.domain.Address

fun randomAddressString() = EthConverter.convert(randomAddress())

fun randomEthAddress() = EthAddress(EthBlockchainDto.ETHEREUM, randomAddressString())
fun randomPolygonAddress() = EthAddress(EthBlockchainDto.POLYGON, randomAddressString())

fun randomEthPartDto() = randomEthPartDto(randomAddress())
fun randomEthPartDto(account: Address) = PartDto(account, randomInt())

fun randomEthItemIdShortValue() = "${randomAddressString()}:${randomBigInt()}"
fun randomEthItemIdFullValue() = "ETHEREUM:${randomEthItemIdShortValue()}"
fun randomPolygonItemIdFullValue() = "POLYGON:${randomEthItemIdShortValue()}"

fun randomEthItemId() = EthItemIdProvider.parseFull(randomEthItemIdFullValue())

fun randomEthOwnershipIdShortValue() = "${randomEthItemIdShortValue()}:${randomAddressString()}"
fun randomEthOwnershipIdFullValue() = "ETHEREUM:${randomEthOwnershipIdShortValue()}"
fun randomPolygonOwnershipIdFullValue() = "POLYGON:${randomEthOwnershipIdShortValue()}"

fun randomEthOwnershipId() = EthOwnershipIdProvider.parseFull(randomEthOwnershipIdFullValue())
fun randomEthOwnershipId(itemId: EthItemIdDto) = randomEthOwnershipId(itemId, randomAddressString())
fun randomEthOwnershipId(itemId: EthItemIdDto, owner: String): EthOwnershipIdDto {
    return EthOwnershipIdDto(
        value = "${itemId.value}:${owner}",
        token = itemId.token,
        tokenId = itemId.tokenId,
        owner = EthAddress(EthBlockchainDto.ETHEREUM, owner),
        blockchain = EthBlockchainDto.ETHEREUM
    )
}

fun randomEthOrderId() = EthOrderIdDto(randomWord(), EthBlockchainDto.ETHEREUM)
fun randomEthOrderIdFullValue() = "ETHEREUM:${randomWord()}"
fun randomEthOrderIdFullValue(hash: Word) = "ETHEREUM:${EthConverter.convert(hash)}"

fun randomPolygonOrderId() = EthOrderIdDto(randomWord(), EthBlockchainDto.POLYGON)
fun randomPolygonOrderIdFullValue() = "POLYGON:${randomWord()}"
fun randomPolygonOrderIdFullValue(hash: Word) = "POLYGON:${EthConverter.convert(hash)}"

fun randomEthNftItemDto() = randomEthNftItemDto(randomEthItemId(), randomEthPartDto())
fun randomEthNftItemDto(itemId: EthItemIdDto, vararg creators: PartDto): NftItemDto {
    return NftItemDto(
        id = itemId.value,
        contract = Address.apply(itemId.token.value),
        tokenId = itemId.tokenId,
        creators = creators.asList(),
        supply = randomBigInt(),
        lazySupply = randomBigInt(),
        royalties = emptyList(),
        date = nowMillis(),
        owners = emptyList(),
        pending = emptyList(),
        deleted = false,
        meta = null
    )
}

fun randomEthNftOwnershipDto() = randomEthNftOwnershipDto(randomEthOwnershipId())
fun randomEthNftOwnershipDto(ownershipId: EthOwnershipIdDto) = randomEthNftOwnershipDto(
    EthItemIdProvider.parseShort("${ownershipId.token.value}:${ownershipId.tokenId}", EthBlockchainDto.ETHEREUM),
    PartDto(Address.apply(ownershipId.owner.value), randomInt())
)

fun randomEthNftOwnershipDto(itemId: EthItemIdDto, creator: PartDto): NftOwnershipDto {
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
        pending = emptyList()
    )
}

fun randomEthAssetErc721() = randomEthAssetErc721(randomEthItemId())
fun randomEthAssetErc721(itemId: EthItemIdDto) = AssetDto(
    Erc721AssetTypeDto(Address.apply(itemId.token.value), itemId.tokenId),
    randomBigInt()
)

fun randomEthAssetErc20() = randomEthAssetErc20(randomAddress())
fun randomEthAssetErc20(address: Address) = AssetDto(Erc20AssetTypeDto(address), randomBigInt())

fun randomEthAssetErc1155(itemId: EthItemIdDto) = AssetDto(
    Erc1155AssetTypeDto(Address.apply(itemId.token.value), itemId.tokenId),
    randomBigInt()
)

fun randomEthLegacyOrderDto() = randomEthLegacyOrderDto(randomEthAssetErc721(), randomAddress(), randomEthAssetErc20())
fun randomEthLegacyOrderDto(itemId: EthItemIdDto) = randomEthLegacyOrderDto(itemId, randomAddress())
fun randomEthLegacyOrderDto(itemId: EthItemIdDto, maker: Address) = randomEthLegacyOrderDto(
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
        makeStock = randomBigInt(),
        cancelled = false,
        salt = Word.apply(randomWord()),
        data = OrderDataLegacyDto(randomInt()),
        signature = null,
        createdAt = nowMillis(),
        lastUpdateAt = nowMillis(),
        pending = emptyList(),
        hash = Word.apply(randomWord()),
        makeBalance = randomBigInt(),
        makePriceUsd = randomBigInt().toBigDecimal(),
        takePriceUsd = randomBigInt().toBigDecimal(),
        start = null,
        end = null,
        priceHistory = listOf()
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

fun randomEthOrderBidActivity(): OrderActivityBidDto {
    return OrderActivityBidDto(
        id = randomString(),
        date = nowMillis(),
        source = OrderActivityDto.Source.RARIBLE,
        hash = Word.apply(randomWord()),
        maker = randomAddress(),
        make = AssetDto(Erc20AssetTypeDto(randomAddress()), randomBigInt()),
        take = AssetDto(Erc20AssetTypeDto(randomAddress()), randomBigInt()),
        price = randomBigDecimal(),
        priceUsd = randomBigDecimal()
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

/*
fun randomEthOpenSeaV1OrderDto(itemId: ItemId) = randomEthOpenSeaV1OrderDto(itemId, randomAddress())
fun randomEthOpenSeaV1OrderDto(itemId: ItemId, maker: Address) = randomEthOpenSeaV1OrderDto(
    randomAssetErc721(itemId),
    maker,
    randomAssetErc20()
)

fun randomEthOpenSeaV1OrderDto(make: AssetDto, maker: Address, take: AssetDto): OpenSeaV1OrderDto {
    return OpenSeaV1OrderDto(
        maker = maker,
        taker = randomAddress(),
        make = make,
        take = take,
        fill = randomBigInt(),
        makeStock = randomBigInt(),
        cancelled = false,
        salt = Word.apply(RandomUtils.nextBytes(32)),
        data = randomOrderOpenSeaV1DataV1Dto(),
        signature = null,
        createdAt = nowMillis(),
        lastUpdateAt = nowMillis(),
        pending = emptyList(),
        hash = Word.apply(RandomUtils.nextBytes(32)),
        makeBalance = randomBigInt(),
        makePriceUsd = randomBigInt().toBigDecimal(),
        takePriceUsd = randomBigInt().toBigDecimal(),
        start = null,
        end = null,
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
*/