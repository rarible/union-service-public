package com.rarible.protocol.union.test.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBoolean
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.*
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.parser.ItemIdParser
import java.math.BigInteger

fun randomFlowContract() = randomString(12)
fun randomFlowAddress() = UnionAddressConverter.convert(BlockchainDto.FLOW, randomLong().toString())

fun randomFlowItemId() = ItemIdDto(BlockchainDto.FLOW, randomFlowContract(), randomLong().toBigInteger())
fun randomFlowItemIdShortValue() = randomFlowItemId().value
fun randomFlowItemIdFullValue() = randomFlowItemId().fullId()

fun randomFlowOwnershipId() = randomFlowOwnershipId(randomFlowItemId())
fun randomFlowOwnershipId(itemId: ItemIdDto) = randomFlowOwnershipId(itemId, randomFlowAddress().value)
fun randomFlowOwnershipId(itemId: ItemIdDto, owner: String): OwnershipIdDto {
    return OwnershipIdDto(
        contract = itemId.contract,
        tokenId = itemId.tokenId,
        owner = UnionAddressConverter.convert(BlockchainDto.FLOW, owner),
        blockchain = BlockchainDto.FLOW
    )
}

fun randomFlowNftItemDto() = randomFlowNftItemDto(randomFlowItemId(), randomString())
fun randomFlowNftItemDto(itemId: ItemIdDto) = randomFlowNftItemDto(itemId, randomString())
fun randomFlowNftItemDto(itemId: ItemIdDto, creator: String): FlowNftItemDto {
    return FlowNftItemDto(
        id = itemId.value,
        collection = itemId.contract,
        tokenId = itemId.tokenId,
        mintedAt = nowMillis(),
        lastUpdatedAt = nowMillis(),
        meta = randomFlowMetaDto(),
        creators = listOf(FlowCreatorDto(creator, randomBigDecimal(0, 2))),
        owner = randomString(),
        royalties = listOf(FlowRoyaltyDto(randomString(), randomBigDecimal(0, 2))),
        supply = randomBigInt(),
        deleted = randomBoolean()
    )
}

fun randomFlowMetaDto(): MetaDto {
    return MetaDto(
        description = randomString(),
        name = randomString(),
        raw = randomString(),
        attributes = listOf(randomFlowMetaAttributeDto()),
        contents = listOf(randomString())
    )
}

fun randomFlowMetaAttributeDto(): MetaAttributeDto {
    return MetaAttributeDto(
        key = randomString(),
        value = randomString()
    )
}

fun randomFlowNftOwnershipDto() = randomFlowNftOwnershipDto(randomFlowOwnershipId())
fun randomFlowNftOwnershipDto(itemId: ItemIdDto) = randomFlowNftOwnershipDto(randomFlowOwnershipId(itemId))
fun randomFlowNftOwnershipDto(ownershipId: OwnershipIdDto) = randomFlowNftOwnershipDto(
    ItemIdParser.parseShort("${ownershipId.contract}:${ownershipId.tokenId}", BlockchainDto.FLOW),
    ownershipId.owner.value
)

fun randomFlowNftOwnershipDto(itemId: ItemIdDto, creator: String): FlowNftOwnershipDto {
    val ownershipId = randomFlowOwnershipId(itemId, creator)
    return FlowNftOwnershipDto(
        id = ownershipId.value,
        contract = ownershipId.contract,
        tokenId = ownershipId.tokenId,
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
        id = randomLong(),
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
            payouts = listOf(PayInfoDto(randomString(), randomBigDecimal(0, 2))),
            originalFees = listOf(PayInfoDto(randomString(), randomBigDecimal(0, 2)))
        ),
        collection = randomFlowContract(),
        lastUpdateAt = nowMillis(),
        makeStock = randomBigInt()
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
        left = randomFlowOrderActivityMatchSideDto().copy(type = FlowOrderActivityMatchSideDto.Type.BID),
        right = randomFlowOrderActivityMatchSideDto().copy(type = FlowOrderActivityMatchSideDto.Type.SELL),
        transactionHash = randomString(),
        blockHash = randomString(),
        blockNumber = randomLong(),
        logIndex = randomInt()
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
        price = randomBigDecimal()
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
        price = randomBigDecimal()
    )
}


fun randomFlowCancelListActivityDto(): FlowNftOrderActivityCancelListDto {
    return FlowNftOrderActivityCancelListDto(
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
        logIndex = randomInt()
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
        logIndex = randomInt()
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
        logIndex = randomInt()
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
        logIndex = randomInt()
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
        logIndex = randomInt()
    )
}

fun randomFlowOrderActivityMatchSideDto(): FlowOrderActivityMatchSideDto {
    return FlowOrderActivityMatchSideDto(
        maker = randomFlowAddress().value,
        asset = randomFlowAsset(),
        type = FlowOrderActivityMatchSideDto.Type.values()[randomInt(FlowOrderActivityMatchSideDto.Type.values().size)]
    )
}
