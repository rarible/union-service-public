package com.rarible.protocol.union.test.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.*
import com.rarible.protocol.dto.*
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.UnionItemIdDto
import com.rarible.protocol.union.dto.UnionOwnershipIdDto
import com.rarible.protocol.union.dto.parser.UnionItemIdParser
import java.math.BigInteger

fun randomFlowContract() = UnionAddress(BlockchainDto.FLOW, randomString(12))
fun randomFlowAddress() = UnionAddress(BlockchainDto.FLOW, randomString(16))

fun randomFlowItemId() = UnionItemIdDto(BlockchainDto.FLOW, randomFlowContract(), randomLong().toBigInteger())
fun randomFlowItemIdShortValue() = randomFlowItemId().value
fun randomFlowItemIdFullValue() = randomFlowItemId().fullId()

fun randomFlowOwnershipId() = randomFlowOwnershipId(randomFlowItemId())
fun randomFlowOwnershipId(itemId: UnionItemIdDto) = randomFlowOwnershipId(itemId, randomFlowAddress().value)
fun randomFlowOwnershipId(itemId: UnionItemIdDto, owner: String): UnionOwnershipIdDto {
    return UnionOwnershipIdDto(
        token = UnionAddress(BlockchainDto.FLOW, itemId.token.value),
        tokenId = itemId.tokenId,
        owner = UnionAddress(BlockchainDto.FLOW, owner),
        blockchain = BlockchainDto.FLOW
    )
}

fun randomFlowNftItemDto() = randomFlowNftItemDto(randomFlowItemId(), randomString())
fun randomFlowNftItemDto(itemId: UnionItemIdDto) = randomFlowNftItemDto(itemId, randomString())
fun randomFlowNftItemDto(itemId: UnionItemIdDto, creator: String): FlowNftItemDto {
    return FlowNftItemDto(
        id = itemId.value,
        collection = itemId.token.value,
        tokenId = itemId.tokenId,
        mintedAt = nowMillis(),
        lastUpdatedAt = nowMillis(),
        meta = randomFlowMetaDto(),
        creators = listOf(FlowCreatorDto(creator, randomBigDecimal())),
        owners = listOf(randomString()),
        royalties = listOf(FlowRoyaltyDto(randomString(), randomBigDecimal())),
        metaUrl = randomString(),
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
        contents = listOf(randomFlowMetaContentDto())
    )
}

fun randomFlowMetaAttributeDto(): MetaAttributeDto {
    return MetaAttributeDto(
        key = randomString(),
        value = randomString()
    )
}

fun randomFlowMetaContentDto(): MetaContentDto {
    return MetaContentDto(
        contentType = randomString(),
        url = randomString(),
        attributes = listOf(randomFlowMetaAttributeDto())
    )
}

fun randomFlowNftOwnershipDto() = randomFlowNftOwnershipDto(randomFlowOwnershipId())
fun randomFlowNftOwnershipDto(ownershipId: UnionOwnershipIdDto) = randomFlowNftOwnershipDto(
    UnionItemIdParser.parseShort("${ownershipId.token.value}:${ownershipId.tokenId}", BlockchainDto.FLOW),
    ownershipId.owner.value
)

fun randomFlowNftOwnershipDto(itemId: UnionItemIdDto, creator: String): FlowNftOwnershipDto {
    val ownershipId = randomFlowOwnershipId(itemId, creator)
    return FlowNftOwnershipDto(
        id = ownershipId.value,
        contract = ownershipId.token.value,
        tokenId = ownershipId.tokenId.toLong(),
        owner = ownershipId.owner.value,
        creators = listOf(PayInfoDto(creator, randomBigDecimal())),
        createdAt = nowMillis()
    )
}

fun randomFlowCollectionDto() = randomFlowCollectionDto(randomString())
fun randomFlowCollectionDto(id: String): FlowNftCollectionDto {
    return FlowNftCollectionDto(
        id = id,
        name = randomString(),
        symbol = randomString(2),
        owner = randomString()
    )
}

fun randomFlowV1OrderDto(): FlowOrderDto {
    return FlowOrderDto(
        id = randomLong(),
        itemId = randomFlowItemIdShortValue(),
        maker = randomFlowAddress().value,
        taker = randomFlowAddress().value,
        make = randomFlowAsset(),
        take = randomFlowFungibleAsset(),
        fill = randomBigDecimal(),
        cancelled = randomBoolean(),
        createdAt = nowMillis(),
        amount = randomBigDecimal(),
        priceUsd = randomBigDecimal(),
        data = FlowOrderDataDto(
            payouts = listOf(PayInfoDto(randomString(), randomBigDecimal())),
            originalFees = listOf(PayInfoDto(randomString(), randomBigDecimal()))
        ),
        collection = randomFlowContract().value,
        lastUpdateAt = nowMillis(),
        offeredNftId = randomString(),
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


fun randomFlowCancelListActivityDto(): FlowNftOrderActivityCancelListDto {
    return FlowNftOrderActivityCancelListDto(
        id = randomString(),
        date = nowMillis(),
        hash = randomString(),
        maker = randomString(),
        make = FlowAssetFungibleDto(randomString(), randomBigDecimal()),
        take = FlowAssetFungibleDto(randomString(), randomBigDecimal()),
        price = randomBigDecimal()
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
