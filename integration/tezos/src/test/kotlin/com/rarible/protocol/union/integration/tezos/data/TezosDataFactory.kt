package com.rarible.protocol.union.integration.tezos.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBoolean
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.tezos.dto.AssetDto
import com.rarible.protocol.tezos.dto.BurnDto
import com.rarible.protocol.tezos.dto.FTAssetTypeDto
import com.rarible.protocol.tezos.dto.MintDto
import com.rarible.protocol.tezos.dto.NFTAssetTypeDto
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
import com.rarible.protocol.tezos.dto.OrderRaribleV2DataV1Dto
import com.rarible.protocol.tezos.dto.OrderStatusDto
import com.rarible.protocol.tezos.dto.PartDto
import com.rarible.protocol.tezos.dto.TransferDto
import com.rarible.protocol.tezos.dto.XTZAssetTypeDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.parser.ItemIdParser

fun randomTezosContract() = randomString(12)
fun randomTezosAddress() = UnionAddressConverter.convert(BlockchainDto.TEZOS, randomString())

fun randomTezosItemId() = ItemIdDto(BlockchainDto.TEZOS, randomTezosContract(), randomLong().toBigInteger())
fun randomTezosItemIdFullValue() = randomTezosItemId().fullId()

fun randomTezosPartDto() = randomTezosPartDto(randomString())
fun randomTezosPartDto(account: String) = PartDto(account, randomInt())

fun randomTezosOwnershipId() = randomTezosOwnershipId(randomTezosItemId())
fun randomTezosOwnershipId(itemId: ItemIdDto) = randomTezosOwnershipId(itemId, randomTezosAddress().value)
fun randomTezosOwnershipId(itemId: ItemIdDto, owner: String): OwnershipIdDto {
    return OwnershipIdDto(
        contract = itemId.contract,
        tokenId = itemId.tokenId,
        owner = UnionAddressConverter.convert(BlockchainDto.TEZOS, owner),
        blockchain = BlockchainDto.TEZOS
    )
}

fun randomTezosNftItemDto() = randomTezosNftItemDto(randomTezosItemId(), randomString())
fun randomTezosNftItemDto(itemId: ItemIdDto) = randomTezosNftItemDto(itemId, randomString())
fun randomTezosNftItemDto(itemId: ItemIdDto, creator: String): NftItemDto {
    return NftItemDto(
        id = itemId.value,
        contract = itemId.contract,
        tokenId = itemId.tokenId,
        mintedAt = nowMillis(),
        date = nowMillis(),
        meta = randomTezosMetaDto(),
        creators = listOf(randomTezosPartDto(creator)),
        owners = emptyList(),
        royalties = listOf(randomTezosPartDto(randomString())),
        supply = randomBigInt(),
        deleted = randomBoolean(),
        lazySupply = randomBigInt()
    )
}

fun randomTezosOwnershipDto() = randomTezosOwnershipDto(randomTezosOwnershipId())
fun randomTezosOwnershipDto(itemId: ItemIdDto) = randomTezosOwnershipDto(
    OwnershipIdDto(
        itemId.blockchain,
        itemId.contract,
        itemId.tokenId,
        UnionAddressConverter.convert(BlockchainDto.TEZOS, randomString())
    )
)

fun randomTezosOwnershipDto(ownershipId: OwnershipIdDto) = randomTezosOwnershipDto(
    ItemIdParser.parseShort("${ownershipId.contract}:${ownershipId.tokenId}", BlockchainDto.TEZOS),
    PartDto(ownershipId.owner.value, randomInt())
)

fun randomTezosOwnershipDto(itemId: ItemIdDto, creator: PartDto): NftOwnershipDto {
    val ownershipId = randomTezosOwnershipId(itemId, creator.account)
    return NftOwnershipDto(
        id = ownershipId.value,
        contract = ownershipId.contract,
        tokenId = ownershipId.tokenId,
        owner = ownershipId.owner.value,
        creators = listOf(creator),
        value = randomBigInt(),
        lazyValue = randomBigInt(),
        date = nowMillis(),
        createdAt = nowMillis()
    )
}

fun randomTezosCollectionDto() = randomTezosCollectionDto(randomString())
fun randomTezosCollectionDto(id: String): NftCollectionDto {
    return NftCollectionDto(
        id = id,
        name = randomString(),
        symbol = randomString(2),
        type = NftCollectionTypeDto.NFT,
        owner = randomString(),
        features = listOf(NftCollectionFeatureDto.values()[randomInt(NftCollectionFeatureDto.values().size)]),
        supports_lazy_mint = true
    )
}

fun randomTezosOrderDto() = randomTezosOrderDto(randomTezosAssetNFT(), randomString(), randomTezosAssetXtz())
fun randomTezosOrderDto(itemId: ItemIdDto) = randomTezosOrderDto(itemId, randomString())
fun randomTezosOrderDto(itemId: ItemIdDto, maker: String) = randomTezosOrderDto(
    randomTezosAssetNFT(itemId),
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
        hash = randomString(32),
        makeBalance = randomBigInt(),
        start = randomInt(),
        end = randomInt(),
        priceHistory = listOf(),
        makerEdpk = randomString(),
        takerEdpk = randomString(),
        status = OrderStatusDto.ACTIVE,
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


fun randomTezosAssetNFT() = randomTezosAssetNFT(randomTezosItemId())
fun randomTezosAssetNFT(itemId: ItemIdDto) = AssetDto(
    assetType = NFTAssetTypeDto(itemId.contract, itemId.tokenId),
    value = randomBigDecimal()
)

fun randomTezosAssetXtz() = AssetDto(
    assetType = XTZAssetTypeDto(),
    value = randomBigDecimal()
)

fun randomTezosAssetFT() = AssetDto(
    assetType = FTAssetTypeDto(randomString()),
    value = randomBigDecimal()
)

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
        make = randomTezosAssetFT(),
        take = randomTezosAssetNFT(),
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
        make = randomTezosAssetNFT(),
        take = randomTezosAssetFT(),
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
        take = randomTezosAssetNFT().assetType
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
        take = randomTezosAssetNFT().assetType
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
        blockNumber = randomBigInt(8)
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
        blockNumber = randomBigInt(8)
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
            blockNumber = randomBigInt(8)
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