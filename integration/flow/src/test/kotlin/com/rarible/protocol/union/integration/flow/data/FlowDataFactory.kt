package com.rarible.protocol.union.integration.flow.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBoolean
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.FlowAssetDto
import com.rarible.protocol.dto.FlowAssetFungibleDto
import com.rarible.protocol.dto.FlowAssetNFTDto
import com.rarible.protocol.dto.FlowBurnDto
import com.rarible.protocol.dto.FlowCreatorDto
import com.rarible.protocol.dto.FlowEventTimeMarkDto
import com.rarible.protocol.dto.FlowEventTimeMarksDto
import com.rarible.protocol.dto.FlowMetaAttributeDto
import com.rarible.protocol.dto.FlowMetaDto
import com.rarible.protocol.dto.FlowMintDto
import com.rarible.protocol.dto.FlowNftCollectionDto
import com.rarible.protocol.dto.FlowNftItemDto
import com.rarible.protocol.dto.FlowNftOrderActivityBidDto
import com.rarible.protocol.dto.FlowNftOrderActivityCancelBidDto
import com.rarible.protocol.dto.FlowNftOrderActivityCancelListDto
import com.rarible.protocol.dto.FlowNftOrderActivityListDto
import com.rarible.protocol.dto.FlowNftOrderActivitySellDto
import com.rarible.protocol.dto.FlowNftOwnershipDto
import com.rarible.protocol.dto.FlowOrderActivityMatchSideDto
import com.rarible.protocol.dto.FlowOrderDataDto
import com.rarible.protocol.dto.FlowOrderDto
import com.rarible.protocol.dto.FlowOrderStatusDto
import com.rarible.protocol.dto.FlowRoyaltyDto
import com.rarible.protocol.dto.FlowTransferDto
import com.rarible.protocol.dto.PayInfoDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import java.math.BigInteger

fun randomFlowContract() = randomString(12)
fun randomFlowAddress() = UnionAddressConverter.convert(BlockchainDto.FLOW, randomLong().toString())

fun randomFlowItemId() = ItemIdDto(BlockchainDto.FLOW, randomFlowContract(), randomLong().toBigInteger())
fun randomFlowItemIdFullValue() = randomFlowItemId().fullId()

fun randomFlowOwnershipId() = randomFlowOwnershipId(randomFlowItemId())
fun randomFlowOwnershipId(itemId: ItemIdDto) = itemId.toOwnership(randomFlowAddress().value)

fun randomFlowNftItemDto() = randomFlowNftItemDto(randomFlowItemId(), randomString())
fun randomFlowNftItemDto(itemId: ItemIdDto) = randomFlowNftItemDto(itemId, randomString())
fun randomFlowNftItemDto(itemId: ItemIdDto, creator: String): FlowNftItemDto {
    val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
    return FlowNftItemDto(
        id = itemId.value,
        collection = contract,
        tokenId = tokenId,
        mintedAt = nowMillis(),
        lastUpdatedAt = nowMillis(),
        creators = listOf(FlowCreatorDto(creator, randomBigDecimal(0, 2))),
        owner = randomString(),
        royalties = listOf(FlowRoyaltyDto(randomString(), randomBigDecimal(0, 2))),
        supply = randomBigInt(),
        deleted = randomBoolean()
    )
}

fun randomFlowItemDtoWithCollection(collectionDto: FlowNftCollectionDto, itemId: ItemIdDto, creator: String): FlowNftItemDto {
    val (_, tokenId) = CompositeItemIdParser.split(itemId.value)
    return FlowNftItemDto(
        id = itemId.value,
        collection = collectionDto.id,
        tokenId = tokenId,
        mintedAt = nowMillis(),
        lastUpdatedAt = nowMillis(),
        creators = listOf(FlowCreatorDto(creator, randomBigDecimal(0, 2))),
        owner = randomString(),
        royalties = listOf(FlowRoyaltyDto(randomString(), randomBigDecimal(0, 2))),
        supply = randomBigInt(),
        deleted = randomBoolean()
    )
}

fun randomFlowMetaDto(): FlowMetaDto {
    return FlowMetaDto(
        description = randomString(),
        name = randomString(),
        raw = randomString(),
        attributes = listOf(randomFlowMetaAttributeDto())
    )
}

fun randomFlowMetaAttributeDto(): FlowMetaAttributeDto {
    return FlowMetaAttributeDto(
        key = randomString(),
        value = randomString()
    )
}

fun randomFlowNftOwnershipDto() = randomFlowNftOwnershipDto(randomFlowOwnershipId())
fun randomFlowNftOwnershipDto(itemId: ItemIdDto) = randomFlowNftOwnershipDto(randomFlowOwnershipId(itemId))
fun randomFlowNftOwnershipDto(ownershipId: OwnershipIdDto) = randomFlowNftOwnershipDto(
    ownershipId.getItemId(),
    ownershipId.owner.value
)

fun randomFlowNftOwnershipDto(itemId: ItemIdDto, creator: String): FlowNftOwnershipDto {
    val ownershipId = itemId.toOwnership(creator)
    val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
    return FlowNftOwnershipDto(
        id = ownershipId.value,
        contract = contract,
        tokenId = tokenId,
        owner = ownershipId.owner.value,
        creators = listOf(PayInfoDto(creator, randomBigDecimal(0, 2))),
        createdAt = nowMillis()
    )
}

fun randomFlowCollectionDto() = randomFlowCollectionDto(randomString())
fun randomFlowCollectionDto(id: String): FlowNftCollectionDto {
    return FlowNftCollectionDto(
        id = id,
        name = randomString(),
        symbol = randomString(2),
        owner = randomString(),
        features = listOf(FlowNftCollectionDto.Features.BURN)
    )
}

fun randomFlowV1OrderDto() = randomFlowV1OrderDto(randomFlowItemId())
fun randomFlowV1OrderDto(itemId: ItemIdDto): FlowOrderDto {
    return FlowOrderDto(
        id = randomString(),
        itemId = itemId.value,
        maker = randomFlowAddress().value,
        taker = randomFlowAddress().value,
        make = randomFlowAsset(),
        take = randomFlowFungibleAsset(),
        fill = randomBigDecimal(),
        status = FlowOrderStatusDto.ACTIVE,
        cancelled = randomBoolean(),
        createdAt = nowMillis(),
        amount = randomBigDecimal(),
        priceUsd = randomBigDecimal(),
        data = FlowOrderDataDto(
            payouts = listOf(PayInfoDto(randomString(), randomBigDecimal(3, 0))),
            originalFees = listOf(PayInfoDto(randomString(), randomBigDecimal(3, 0)))
        ),
        collection = randomFlowContract(),
        lastUpdateAt = nowMillis(),
        makeStock = randomBigDecimal(),
        dbUpdatedAt = nowMillis(),
    )
}

fun randomFlowAsset(): FlowAssetDto {
    return randomFlowNftAsset()
}

fun randomFlowFungibleAsset() = randomFlowFungibleAsset(randomFlowAddress())
fun randomFlowFungibleAsset(contract: UnionAddress) = FlowAssetFungibleDto(
    value = randomBigDecimal(),
    contract = contract.value
)

fun randomFlowNftAsset() = randomFlowNftAsset(randomFlowAddress(), randomBigInt())
fun randomFlowNftAsset(contract: UnionAddress, tokenId: BigInteger) = FlowAssetNFTDto(
    contract = contract.value,
    tokenId = tokenId,
    value = randomBigDecimal()
)

fun randomFlowNftOrderActivitySell(): FlowNftOrderActivitySellDto {
    return FlowNftOrderActivitySellDto(
        id = randomString(),
        date = nowMillis(),
        price = randomBigDecimal(),
        left = randomFlowOrderActivityMatchSideDto()
            .copy(type = FlowOrderActivityMatchSideDto.Type.BID),
        right = randomFlowOrderActivityMatchSideDto(randomFlowFungibleAsset())
            .copy(type = FlowOrderActivityMatchSideDto.Type.SELL),
        transactionHash = randomString(),
        blockHash = randomString(),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        updatedAt = nowMillis(),
    )
}

fun randomFlowNftOrderActivityListDto(): FlowNftOrderActivityListDto {
    return FlowNftOrderActivityListDto(
        id = randomString(),
        date = nowMillis(),
        hash = randomString(),
        maker = randomString(),
        make = FlowAssetFungibleDto(randomString(), randomBigDecimal()),
        take = FlowAssetFungibleDto(randomString(), randomBigDecimal()),
        price = randomBigDecimal(),
        updatedAt = nowMillis(),
    )
}

fun randomFlowNftOrderActivityBidDto(): FlowNftOrderActivityBidDto {
    return FlowNftOrderActivityBidDto(
        id = randomString(),
        date = nowMillis(),
        hash = randomString(),
        maker = randomString(),
        make = FlowAssetFungibleDto(randomString(), randomBigDecimal()),
        take = FlowAssetFungibleDto(randomString(), randomBigDecimal()),
        price = randomBigDecimal(),
        updatedAt = nowMillis(),
    )
}

fun randomFlowCancelListActivityDto(): FlowNftOrderActivityCancelListDto {
    return FlowNftOrderActivityCancelListDto(
        id = randomString(),
        date = nowMillis(),
        hash = randomString(),
        maker = randomString(),
        make = FlowAssetNFTDto(randomString(), randomBigDecimal(), randomBigInt()),
        take = FlowAssetFungibleDto(randomString(), randomBigDecimal()),
        price = randomBigDecimal(),
        transactionHash = randomString(),
        blockHash = randomString(),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        updatedAt = nowMillis(),
    )
}

fun randomFlowCancelBidActivityDto(): FlowNftOrderActivityCancelBidDto {
    return FlowNftOrderActivityCancelBidDto(
        id = randomString(),
        date = nowMillis(),
        hash = randomString(),
        maker = randomString(),
        make = FlowAssetFungibleDto(randomString(), randomBigDecimal()),
        take = FlowAssetFungibleDto(randomString(), randomBigDecimal()),
        price = randomBigDecimal(),
        transactionHash = randomString(),
        blockHash = randomString(),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        updatedAt = nowMillis(),
    )
}

fun randomFlowMintDto(): FlowMintDto {
    return FlowMintDto(
        id = randomString(),
        date = nowMillis(),
        owner = randomString(),
        contract = randomString(),
        tokenId = randomBigInt(),
        value = randomBigInt(),
        transactionHash = randomString(),
        blockHash = randomString(),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        updatedAt = nowMillis(),
    )
}

fun randomFlowTransferDto(): FlowTransferDto {
    return FlowTransferDto(
        id = randomString(),
        date = nowMillis(),
        owner = randomString(),
        contract = randomString(),
        tokenId = randomBigInt(),
        value = randomBigInt(),
        from = randomString(),
        transactionHash = randomString(),
        blockHash = randomString(),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        purchased = false,
        updatedAt = nowMillis(),
    )
}

fun randomFlowBurnDto(): FlowBurnDto {
    return FlowBurnDto(
        id = randomString(),
        date = nowMillis(),
        owner = randomString(),
        contract = randomString(),
        tokenId = randomBigInt(),
        value = randomBigInt(),
        transactionHash = randomString(),
        blockHash = randomString(),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        updatedAt = nowMillis(),
    )
}

fun randomFlowOrderActivityMatchSideDto() = randomFlowOrderActivityMatchSideDto(randomFlowAsset())
fun randomFlowOrderActivityMatchSideDto(asset: FlowAssetDto): FlowOrderActivityMatchSideDto {
    return FlowOrderActivityMatchSideDto(
        maker = randomFlowAddress().value,
        asset = asset,
        type = FlowOrderActivityMatchSideDto.Type.values()[randomInt(FlowOrderActivityMatchSideDto.Type.values().size)]
    )
}

fun randomFlowEventTimeMarks(): FlowEventTimeMarksDto {
    return FlowEventTimeMarksDto(
        source = randomString(),
        marks = listOf(
            FlowEventTimeMarkDto(
                randomString(),
                nowMillis().minusSeconds(randomLong(1000))
            )
        )
    )
}
