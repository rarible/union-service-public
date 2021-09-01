package com.rarible.protocol.union.listener.test.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.dto.*
import com.rarible.protocol.union.dto.EthAddress
import com.rarible.protocol.union.dto.EthItemIdDto
import com.rarible.protocol.union.dto.EthOwnershipIdDto
import com.rarible.protocol.union.dto.serializer.eth.EthItemIdParser
import com.rarible.protocol.union.dto.serializer.eth.EthOwnershipIdParser
import io.daonomic.rpc.domain.Word
import scalether.domain.Address

fun randomEthPartDto() = randomEthPartDto(randomAddress())
fun randomEthPartDto(account: Address) = PartDto(account, randomInt())

fun randomEthItemIdShortValue() = "${randomAddress()}:${randomBigInt()}"
fun randomEthItemIdFullValue() = "ETHEREUM:${randomEthItemIdShortValue()}"

fun randomEthItemId() = EthItemIdParser.parseShort(randomEthItemIdShortValue())

fun randomEthOwnershipIdShortValue() = "${randomEthItemIdShortValue()}:${randomAddress()}"
fun randomEthOwnershipIdFullValue() = "ETHEREUM:${randomEthOwnershipIdShortValue()}"

fun randomEthOwnershipId() = EthOwnershipIdParser.parseShort(randomEthOwnershipIdShortValue())
fun randomEthOwnershipId(itemId: EthItemIdDto) = randomEthOwnershipId(itemId, randomAddress().toString())
fun randomEthOwnershipId(itemId: EthItemIdDto, owner: String): EthOwnershipIdDto {
    return EthOwnershipIdDto(
        value = "${itemId.value}:${owner}",
        token = itemId.token,
        tokenId = itemId.tokenId,
        owner = EthAddress(owner)
    )
}

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
    EthItemIdParser.parseShort("${ownershipId.token.value}:${ownershipId.tokenId}"),
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