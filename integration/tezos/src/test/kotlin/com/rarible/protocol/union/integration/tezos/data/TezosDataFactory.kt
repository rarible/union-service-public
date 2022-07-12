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
import com.rarible.protocol.tezos.dto.NftActTypeDto
import com.rarible.protocol.tezos.dto.NftActivityEltDto
import com.rarible.protocol.tezos.dto.NftCollectionDto
import com.rarible.protocol.tezos.dto.NftCollectionFeatureDto
import com.rarible.protocol.tezos.dto.NftCollectionTypeDto
import com.rarible.protocol.tezos.dto.NftItemAttributeDto
import com.rarible.protocol.tezos.dto.NftItemDto
import com.rarible.protocol.tezos.dto.NftItemMetaDto
import com.rarible.protocol.tezos.dto.NftOwnershipDto
import com.rarible.protocol.tezos.dto.OrderActTypeDto
import com.rarible.protocol.tezos.dto.OrderActivityBidDto
import com.rarible.protocol.tezos.dto.OrderActivityCancelBidDto
import com.rarible.protocol.tezos.dto.OrderActivityCancelListDto
import com.rarible.protocol.tezos.dto.OrderActivityListDto
import com.rarible.protocol.tezos.dto.OrderActivityMatchDto
import com.rarible.protocol.tezos.dto.OrderActivityMatchTypeDto
import com.rarible.protocol.tezos.dto.OrderActivitySideMatchDto
import com.rarible.protocol.tezos.dto.OrderActivitySideTypeDto
import com.rarible.protocol.tezos.dto.OrderDto
import com.rarible.protocol.tezos.dto.OrderRaribleV2DataV1Dto
import com.rarible.protocol.tezos.dto.OrderStatusDto
import com.rarible.protocol.tezos.dto.PartDto
import com.rarible.protocol.tezos.dto.TransferDto
import com.rarible.protocol.tezos.dto.XTZAssetTypeDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.tzkt.model.Alias
import com.rarible.tzkt.model.Contract
import com.rarible.tzkt.model.TokenBalance
import com.rarible.tzkt.model.TokenInfo
import java.time.OffsetDateTime

fun randomTezosContract() = randomString(12)
fun randomTezosAddress() = UnionAddressConverter.convert(BlockchainDto.TEZOS, randomString())

fun randomTezosItemId() = ItemIdDto(BlockchainDto.TEZOS, randomTezosContract() + ":" + randomLong().toBigInteger())
fun randomTezosItemIdFullValue() = randomTezosItemId().fullId()

fun randomTezosPartDto() = randomTezosPartDto(randomString())
fun randomTezosPartDto(account: String) = PartDto(account, randomInt())

fun randomTezosOwnershipId() = randomTezosOwnershipId(randomTezosItemId())
fun randomTezosOwnershipId(itemId: ItemIdDto) = itemId.toOwnership(randomTezosAddress().value)

fun randomTezosNftItemDto() = randomTezosNftItemDto(randomTezosItemId(), randomString())
fun randomTezosNftItemDto(itemId: ItemIdDto) = randomTezosNftItemDto(itemId, randomString())
fun randomTezosNftItemDto(itemId: ItemIdDto, creator: String): NftItemDto {
    val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
    return NftItemDto(
        id = itemId.value,
        contract = contract,
        tokenId = tokenId,
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
    itemId.toOwnership(randomString())
)

fun randomTezosOwnershipDto(ownershipId: OwnershipIdDto) = randomTezosOwnershipDto(
    ownershipId.getItemId(),
    PartDto(ownershipId.owner.value, randomInt())
)

fun randomTezosOwnershipDto(itemId: ItemIdDto, creator: PartDto): NftOwnershipDto {
    val ownershipId = itemId.toOwnership(creator.account)
    val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
    return NftOwnershipDto(
        id = ownershipId.value,
        contract = contract,
        tokenId = tokenId,
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
        salt = randomBigInt(32),
        data = OrderRaribleV2DataV1Dto(randomString(), listOf(randomTezosPartDto()), listOf(randomTezosPartDto())),
        signature = randomString(16),
        createdAt = nowMillis(),
        lastUpdateAt = nowMillis(),
        hash = randomString(32),
        makeBalance = randomBigInt(),
        start = randomInt().toLong(),
        end = randomInt().toLong(),
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
fun randomTezosAssetNFT(itemId: ItemIdDto): AssetDto {
    val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
    return AssetDto(
        assetType = NFTAssetTypeDto(contract, tokenId),
        value = randomBigDecimal()
    )
}

fun randomTezosAssetXtz() = AssetDto(
    assetType = XTZAssetTypeDto(),
    value = randomBigDecimal()
)

fun randomTezosAssetFT() = AssetDto(
    assetType = FTAssetTypeDto(randomString()),
    value = randomBigDecimal()
)

fun randomTezosOrderActivityMatch(): OrderActTypeDto {
    return OrderActTypeDto(
        id = randomString(),
        date = nowMillis(),
        source = "RARIBLE",
        type = OrderActivityMatchDto(
            left = randomTezosOrderActivityMatchSide(),
            right = randomTezosOrderActivityMatchSide(),
            price = randomBigDecimal(),
            transactionHash = randomString(),
            blockHash = randomString(),
            blockNumber = randomBigInt(8),
            logIndex = randomInt(),
            type = OrderActivityMatchTypeDto.SELL
        )
    )
}

fun randomTezosOrderBidActivity(): OrderActTypeDto {
    return OrderActTypeDto(
        id = randomString(),
        date = nowMillis(),
        source = "RARIBLE",
        type = OrderActivityBidDto(
            hash = randomString(16),
            maker = randomString(),
            make = randomTezosAssetFT(),
            take = randomTezosAssetNFT(),
            price = randomBigDecimal()
        )
    )
}

fun randomTezosOrderListActivity(): OrderActTypeDto {
    return OrderActTypeDto(
        id = randomString(),
        date = nowMillis(),
        source = "RARIBLE",
        type = OrderActivityListDto(
            hash = randomString(16),
            maker = randomString(),
            make = randomTezosAssetNFT(),
            take = randomTezosAssetFT(),
            price = randomBigDecimal()
        )
    )
}

fun randomTezosOrderActivityCancelBid(): OrderActTypeDto {
    return OrderActTypeDto(
        id = randomString(),
        date = nowMillis(),
        source = "RARIBLE",
        type = OrderActivityCancelBidDto(
            transactionHash = randomString(),
            blockHash = randomString(),
            blockNumber = randomBigInt(8),
            logIndex = randomInt(),
            maker = randomString(),
            hash = randomString(16),
            make = randomTezosAssetXtz().assetType,
            take = randomTezosAssetNFT().assetType
        )
    )
}

fun randomTezosOrderActivityCancelList(): OrderActTypeDto {
    return OrderActTypeDto(
        id = randomString(),
        date = nowMillis(),
        source = "RARIBLE",
        type = OrderActivityCancelListDto(
            transactionHash = randomString(),
            blockHash = randomString(),
            blockNumber = randomBigInt(8),
            logIndex = randomInt(),
            maker = randomString(),
            hash = randomString(16),
            make = randomTezosAssetXtz().assetType,
            take = randomTezosAssetNFT().assetType
        )
    )
}

fun randomTezosItemMintActivity(): NftActTypeDto {
    return NftActTypeDto(
        id = randomString(),
        date = nowMillis(),
        source = "RARIBLE",
        type = MintDto(
            owner = randomString(),
            contract = randomString(),
            tokenId = randomBigInt(),
            value = randomBigInt().toBigDecimal(),
            transactionHash = randomString(),
            blockHash = randomString(),
            blockNumber = randomBigInt(8)
        )
    )
}

fun randomTezosItemBurnActivity(): NftActTypeDto {
    return NftActTypeDto(
        id = randomString(),
        date = nowMillis(),
        source = "RARIBLE",
        type = BurnDto(
            owner = randomString(),
            contract = randomString(),
            tokenId = randomBigInt(),
            value = randomBigInt().toBigDecimal(),
            transactionHash = randomString(),
            blockHash = randomString(),
            blockNumber = randomBigInt(8)
        )
    )
}

fun randomTezosItemTransferActivity(): NftActTypeDto {
    return NftActTypeDto(
        id = randomString(),
        date = nowMillis(),
        source = "RARIBLE",
        type = TransferDto(
            from = randomString(),
            elt = NftActivityEltDto(
                owner = randomString(),
                contract = randomString(),
                tokenId = randomBigInt(),
                value = randomBigInt().toBigDecimal(),
                transactionHash = randomString(),
                blockHash = randomString(),
                blockNumber = randomBigInt(8)
            )
        )
    )
}

fun randomTezosOrderActivityMatchSide(): OrderActivitySideMatchDto {
    return OrderActivitySideMatchDto(
        maker = randomString(),
        hash = randomString(16),
        asset = randomTezosAssetFT(),
        type = OrderActivitySideTypeDto.values()[randomInt(OrderActivitySideTypeDto.values().size)]
    )
}

fun randomTzktContract(address: String): Contract {
    return Contract(
        type = "contract",
        alias = "",
        name = "test",
        symbol = null,
        balance = 1L,
        address = address,
        tzips = listOf("fa2"),
        kind = "",
        numContracts = 1,
        activeTokensCount = 1,
        tokenBalancesCount = 1,
        tokenTransfersCount = 1,
        numDelegations = 1,
        numOriginations = 1,
        numTransactions = 1,
        numReveals = 1,
        numMigrations = 1
    )
}

fun randomTzktTokenBalance(ownerId: OwnershipIdDto): TokenBalance {
    return TokenBalance(
        id = randomInt(),
        account = Alias(
            alias = null,
            address = ownerId.owner.value
        ),
        token = TokenInfo(
            id = 718165,
            contract = Alias(
                alias = null,
                address = ownerId.itemIdValue.split(":").first()
            ),
            tokenId = ownerId.itemIdValue.split(":").last(),
        ),
        balance = "1",
        transfersCount = randomInt(),
        firstLevel = 1,
        firstTime = OffsetDateTime.now(),
        lastLevel = 2,
        lastTime = OffsetDateTime.now()
    )
}
