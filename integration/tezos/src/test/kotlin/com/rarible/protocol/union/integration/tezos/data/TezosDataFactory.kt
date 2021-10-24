package com.rarible.protocol.union.test.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBoolean
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.tezos.dto.AssetDto
import com.rarible.protocol.tezos.dto.BurnDto
import com.rarible.protocol.tezos.dto.FA_1_2AssetTypeDto
import com.rarible.protocol.tezos.dto.FA_2AssetTypeDto
import com.rarible.protocol.tezos.dto.MintDto
import com.rarible.protocol.tezos.dto.NftActivityEltDto
import com.rarible.protocol.tezos.dto.NftCollectionDto
import com.rarible.protocol.tezos.dto.NftCollectionFeatureDto
import com.rarible.protocol.tezos.dto.NftCollectionTypeDto
import com.rarible.protocol.tezos.dto.NftItemAttributeDto
import com.rarible.protocol.tezos.dto.NftItemDto
import com.rarible.protocol.tezos.dto.NftItemMetaDto
import com.rarible.protocol.tezos.dto.NftOwnershipDto
import com.rarible.protocol.tezos.dto.OrderActivityBidDto
import com.rarible.protocol.tezos.dto.OrderActivityCancelBidDto
import com.rarible.protocol.tezos.dto.OrderActivityCancelListDto
import com.rarible.protocol.tezos.dto.OrderActivityListDto
import com.rarible.protocol.tezos.dto.OrderActivityMatchDto
import com.rarible.protocol.tezos.dto.OrderActivitySideMatchDto
import com.rarible.protocol.tezos.dto.OrderActivitySideTypeDto
import com.rarible.protocol.tezos.dto.OrderDto
import com.rarible.protocol.tezos.dto.OrderPriceHistoryRecordDto
import com.rarible.protocol.tezos.dto.OrderRaribleV2DataV1Dto
import com.rarible.protocol.tezos.dto.OrderStatusDto
import com.rarible.protocol.tezos.dto.PartDto
import com.rarible.protocol.tezos.dto.TransferDto
import com.rarible.protocol.tezos.dto.XTZAssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.parser.ItemIdParser
import java.lang.Long
import kotlin.String
import kotlin.toBigInteger

fun randomTezosContract() = UnionAddress(BlockchainDto.TEZOS, randomString(12))
fun randomTezosAddress() = UnionAddress(BlockchainDto.TEZOS, Long.toHexString(randomLong()))

fun randomTezosItemId() = ItemIdDto(BlockchainDto.TEZOS, randomTezosContract(), randomLong().toBigInteger())
fun randomTezosItemIdFullValue() = randomTezosItemId().fullId()

fun randomTezosPartDto() = randomTezosPartDto(randomString())
fun randomTezosPartDto(account: String) = PartDto(account, randomInt())

fun randomTezosOwnershipId() = randomTezosOwnershipId(randomTezosItemId())
fun randomTezosOwnershipId(itemId: ItemIdDto) = randomTezosOwnershipId(itemId, randomTezosAddress().value)
fun randomTezosOwnershipId(itemId: ItemIdDto, owner: String): OwnershipIdDto {
    return OwnershipIdDto(
        token = UnionAddress(BlockchainDto.TEZOS, itemId.token.value),
        tokenId = itemId.tokenId,
        owner = UnionAddress(BlockchainDto.TEZOS, owner),
        blockchain = BlockchainDto.TEZOS
    )
}

fun randomTezosNftItemDto() = randomTezosNftItemDto(randomTezosItemId(), randomString())
fun randomTezosNftItemDto(itemId: ItemIdDto) = randomTezosNftItemDto(itemId, randomString())
fun randomTezosNftItemDto(itemId: ItemIdDto, creator: String): NftItemDto {
    return NftItemDto(
        id = itemId.value,
        contract = itemId.token.value,
        tokenId = itemId.tokenId,
        mintedAt = nowMillis(),
        date = nowMillis(),
        meta = randomTezosMetaDto(),
        creators = listOf(randomTezosPartDto(creator)),
        owners = listOf(randomString()),
        royalties = listOf(randomTezosPartDto(randomString())),
        supply = randomBigInt(),
        deleted = randomBoolean(),
        lazySupply = randomBigInt(),
        pending = emptyList() // TODO not supported yet
    )
}

fun randomTezosOwnershipDto() = randomTezosOwnershipDto(randomTezosOwnershipId())
fun randomTezosOwnershipDto(itemId: ItemIdDto) = randomTezosOwnershipDto(
    OwnershipIdDto(
        itemId.blockchain,
        itemId.token,
        itemId.tokenId,
        UnionAddress(BlockchainDto.TEZOS, randomString())
    )
)

fun randomTezosOwnershipDto(ownershipId: OwnershipIdDto) = randomTezosOwnershipDto(
    ItemIdParser.parseShort("${ownershipId.token.value}:${ownershipId.tokenId}", BlockchainDto.ETHEREUM),
    PartDto(ownershipId.owner.value, randomInt())
)

fun randomTezosOwnershipDto(itemId: ItemIdDto, creator: PartDto): NftOwnershipDto {
    val ownershipId = randomTezosOwnershipId(itemId, creator.account.toString())
    return NftOwnershipDto(
        id = ownershipId.value,
        contract = ownershipId.token.value,
        tokenId = ownershipId.tokenId,
        owner = ownershipId.owner.value,
        creators = listOf(creator),
        value = randomBigInt(),
        lazyValue = randomBigInt(),
        date = nowMillis(),
        pending = listOf()
    )
}

fun randomTezosCollectionDto() = randomTezosCollectionDto(randomString())
fun randomTezosCollectionDto(id: String): NftCollectionDto {
    return NftCollectionDto(
        id = id,
        name = randomString(),
        symbol = randomString(2),
        type = NftCollectionTypeDto.FA_2,
        owner = randomString(),
        features = listOf(NftCollectionFeatureDto.values()[randomInt(NftCollectionFeatureDto.values().size)]),
        supports_lazy_mint = true
    )
}

fun randomTezosOrderDto() = randomTezosOrderDto(randomTezosAssetFa2(), randomString(), randomTezosAssetXtz())
fun randomTezosOrderDto(itemId: ItemIdDto) = randomTezosOrderDto(itemId, randomString())
fun randomTezosOrderDto(itemId: ItemIdDto, maker: String) = randomTezosOrderDto(
    randomTezosAssetFa2(itemId),
    maker,
    randomTezosAssetXtz()
)

fun randomTezosOrderDto(make: AssetDto, maker: String, take: AssetDto): OrderDto {
    return OrderDto(
        maker = maker,
        taker = randomString(),
        make = make,
        take = take,
        fill = randomBigInt(),
        makeStock = randomBigInt(),
        cancelled = false,
        salt = randomString(32),
        data = OrderRaribleV2DataV1Dto(randomString(), listOf(randomTezosPartDto()), listOf(randomTezosPartDto())),
        signature = randomString(16),
        createdAt = nowMillis(),
        lastUpdateAt = nowMillis(),
        pending = listOf(), // TODO add later maybe
        hash = randomString(32),
        makeBalance = randomBigInt(),
        start = randomInt(),
        end = randomInt(),
        priceHistory = listOf(randomTezosOrderPriceHistoryRecordDto()),
        makerEdpk = randomString(),
        takerEdpk = randomString(),
        status = OrderStatusDto.OACTIVE,
        type = OrderDto.Type.RARIBLE_V2
    )
}

fun randomTezosMetaDto(): NftItemMetaDto {
    return NftItemMetaDto(
        name = randomString(),
        description = randomString(),
        attributes = listOf(randomTezosItemMetaAttribute()),
        image = randomString(),
        animation = randomString()
    )
}

fun randomTezosItemMetaAttribute(): NftItemAttributeDto {
    return NftItemAttributeDto(
        key = randomString(),
        value = randomString(),
        type = randomString(),
        format = randomString()
    )
}


fun randomTezosAssetFa2() = randomTezosAssetFa2(randomTezosItemId())
fun randomTezosAssetFa2(itemId: ItemIdDto) = AssetDto(
    assetType = FA_2AssetTypeDto(itemId.token.value, itemId.tokenId),
    value = randomBigDecimal()
)

fun randomTezosAssetXtz() = AssetDto(
    assetType = XTZAssetTypeDto(),
    value = randomBigDecimal()
)

fun randomTezosAssetFa12() = AssetDto(
    assetType = FA_1_2AssetTypeDto(randomString()),
    value = randomBigDecimal()
)

fun randomTezosOrderPriceHistoryRecordDto(): OrderPriceHistoryRecordDto {
    return OrderPriceHistoryRecordDto(
        date = nowMillis(),
        makeValue = randomBigDecimal(),
        takeValue = randomBigDecimal()
    )
}

fun randomTezosOrderActivityMatch(): OrderActivityMatchDto {
    return OrderActivityMatchDto(
        id = randomString(),
        date = nowMillis(),
        source = "RARIBLE",
        left = randomTezosOrderActivityMatchSide(),
        right = randomTezosOrderActivityMatchSide(),
        price = randomBigDecimal(),
        transactionHash = randomString(),
        blockHash = randomString(),
        blockNumber = randomBigInt(8),
        logIndex = randomInt()
        //type = 
    )
}

fun randomTezosOrderBidActivity(): OrderActivityBidDto {
    return OrderActivityBidDto(
        id = randomString(),
        date = nowMillis(),
        source = "RARIBLE",
        hash = randomString(16),
        maker = randomString(),
        make = randomTezosAssetFa2(),
        take = randomTezosAssetFa12(),
        price = randomBigDecimal()
    )
}

fun randomTezosOrderListActivity(): OrderActivityListDto {
    return OrderActivityListDto(
        id = randomString(),
        date = nowMillis(),
        source = "RARIBLE",
        hash = randomString(16),
        maker = randomString(),
        make = randomTezosAssetFa2(),
        take = randomTezosAssetFa12(),
        price = randomBigDecimal()
    )
}

fun randomTezosOrderActivityCancelBid(): OrderActivityCancelBidDto {
    return OrderActivityCancelBidDto(
        id = randomString(),
        date = nowMillis(),
        source = "RARIBLE",
        transactionHash = randomString(),
        blockHash = randomString(),
        blockNumber = randomBigInt(8),
        logIndex = randomInt(),
        maker = randomString(),
        hash = randomString(16),
        make = randomTezosAssetXtz().assetType,
        take = randomTezosAssetFa2().assetType
    )
}

fun randomTezosOrderActivityCancelList(): OrderActivityCancelListDto {
    return OrderActivityCancelListDto(
        id = randomString(),
        date = nowMillis(),
        source = "RARIBLE",
        transactionHash = randomString(),
        blockHash = randomString(),
        blockNumber = randomBigInt(8),
        logIndex = randomInt(),
        maker = randomString(),
        hash = randomString(16),
        make = randomTezosAssetXtz().assetType,
        take = randomTezosAssetFa2().assetType
    )
}

fun randomTezosItemMintActivity(): MintDto {
    return MintDto(
        id = randomString(),
        date = nowMillis(),
        source = "RARIBLE",
        owner = randomString(),
        contract = randomString(),
        tokenId = randomBigInt(),
        value = randomBigInt(),
        transactionHash = randomString(),
        blockHash = randomString(),
        blockNumber = randomBigInt(8),
        logIndex = randomInt()
    )
}

fun randomTezosItemBurnActivity(): BurnDto {
    return BurnDto(
        id = randomString(),
        date = nowMillis(),
        source = "RARIBLE",
        owner = randomString(),
        contract = randomString(),
        tokenId = randomBigInt(),
        value = randomBigInt(),
        transactionHash = randomString(),
        blockHash = randomString(),
        blockNumber = randomBigInt(8),
        logIndex = randomInt()
    )
}

fun randomTezosItemTransferActivity(): TransferDto {
    return TransferDto(
        id = randomString(),
        date = nowMillis(),
        source = "RARIBLE",
        from = randomString(),
        elt = NftActivityEltDto(
            owner = randomString(),
            contract = randomString(),
            tokenId = randomBigInt(),
            value = randomBigInt(),
            transactionHash = randomString(),
            blockHash = randomString(),
            blockNumber = randomBigInt(8),
            logIndex = randomInt()
        )
    )
}

fun randomTezosOrderActivityMatchSide(): OrderActivitySideMatchDto {
    return OrderActivitySideMatchDto(
        maker = randomString(),
        hash = randomString(16),
        asset = randomTezosAssetXtz(),
        type = OrderActivitySideTypeDto.values()[randomInt(OrderActivitySideTypeDto.values().size)]
    )
}