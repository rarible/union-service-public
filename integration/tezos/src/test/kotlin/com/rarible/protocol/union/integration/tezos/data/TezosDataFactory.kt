package com.rarible.protocol.union.integration.tezos.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.dipdup.client.core.model.Asset
import com.rarible.dipdup.client.core.model.DipDupActivity
import com.rarible.dipdup.client.core.model.DipDupBurnActivity
import com.rarible.dipdup.client.core.model.DipDupItem
import com.rarible.dipdup.client.core.model.DipDupMintActivity
import com.rarible.dipdup.client.core.model.DipDupOrder
import com.rarible.dipdup.client.core.model.DipDupOrderCancelActivity
import com.rarible.dipdup.client.core.model.DipDupOrderListActivity
import com.rarible.dipdup.client.core.model.DipDupOwnership
import com.rarible.dipdup.client.core.model.DipDupTransferActivity
import com.rarible.dipdup.client.core.model.OrderStatus
import com.rarible.dipdup.client.core.model.Part
import com.rarible.dipdup.client.core.model.TezosPlatform
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.tzkt.model.Alias
import com.rarible.tzkt.model.Contract
import com.rarible.tzkt.model.Token
import com.rarible.tzkt.model.TokenBalance
import com.rarible.tzkt.model.TokenInfo
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

fun randomTezosContract() = randomString(12)
fun randomTezosAddress() = UnionAddressConverter.convert(BlockchainDto.TEZOS, randomString())

fun randomTezosItemId() = ItemIdDto(BlockchainDto.TEZOS, randomTezosContract() + ":" + randomLong().toBigInteger())
fun randomTezosItemIdFullValue() = randomTezosItemId().fullId()

fun randomTezosCollectionDto() = randomTezosCollectionDto(randomString())
fun randomTezosCollectionDto(address: String): Contract {
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

//fun randomTezosPartDto() = randomTezosPartDto(randomString())
//fun randomTezosPartDto(account: String) = PartDto(account, randomInt())

fun randomTezosOwnershipId() = randomTezosOwnershipId(randomTezosItemId())
fun randomTezosOwnershipId(itemId: ItemIdDto) = itemId.toOwnership(randomTezosAddress().value)

fun randomTezosTzktItemDto() = randomTezosTzktItemDto(randomTezosItemId(), randomString())
fun randomTezosTzktItemDto(itemId: ItemIdDto) = randomTezosTzktItemDto(itemId, randomString())
fun randomTezosTzktItemDto(itemId: ItemIdDto, creator: String): Token {
    val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
    return Token(
        id = 1,
        contract = Alias(
            alias = "test name",
            address = contract
        ),
        tokenId = tokenId.toString(),
        firstTime = nowMillis().atOffset(ZoneOffset.UTC),
        lastTime = nowMillis().atOffset(ZoneOffset.UTC),
        totalSupply = "1",
        transfersCount = 1,
        balancesCount = 1,
        holdersCount = 1
    )
}

fun randomTezosDipDupItemDto() = randomTezosDipDupItemDto(randomTezosItemId())
fun randomTezosDipDupItemDto(itemId: ItemIdDto): DipDupItem {
    val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
    return DipDupItem(
        id = itemId.value,
        minted = BigInteger.ONE,
        mintedAt = Instant.now(),
        supply = BigInteger.ONE,
        tokenId = tokenId,
        updated = Instant.now(),
        contract = contract,
        deleted = false,
        tzktId = BigInteger.ONE,
        creators = listOf(Part(randomString(), 10000))
    )
}

fun randomTezosTzktOwnershipDto() = randomTezosTzktOwnershipDto(randomTezosOwnershipId())
fun randomTezosTzktOwnershipDto(ownershipId: OwnershipIdDto) = randomTezosTzktOwnershipDto(
    ownershipId.getItemId(),
    Part(ownershipId.owner.value, randomInt())
)
fun randomTezosTzktOwnershipDto(itemId: ItemIdDto) = randomTezosTzktOwnershipDto(
    itemId.toOwnership(randomString())
)
fun randomTezosTzktOwnershipDto(itemId: ItemIdDto, creator: Part): TokenBalance {
    val ownershipId = itemId.toOwnership(creator.account)
    val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
    return TokenBalance(
        id = randomLong(),
        account = Alias(
            alias = null,
            address = ownershipId.owner.value
        ),
        token = TokenInfo(
            id = 718165,
            contract = Alias(
                alias = null,
                address = contract
            ),
            tokenId = tokenId.toString(),
        ),
        balance = "1",
        transfersCount = randomInt(),
        firstLevel = 1,
        firstTime = OffsetDateTime.now(),
        lastLevel = 2,
        lastTime = OffsetDateTime.now()
    )
}

fun randomTezosDipDupOwnershipDto() = randomTezosDipDupOwnershipDto(randomTezosOwnershipId())
fun randomTezosDipDupOwnershipDto(ownershipId: OwnershipIdDto) = randomTezosDipDupOwnershipDto(
    ownershipId.getItemId(),
    Part(ownershipId.owner.value, randomInt())
)
fun randomTezosDipDupOwnershipDto(itemId: ItemIdDto, creator: Part): DipDupOwnership {
    val ownershipId = itemId.toOwnership(creator.account)
    val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
    return DipDupOwnership(
        id = ownershipId.value,
        updated = Instant.now(),
        created = Instant.now(),
        contract = contract,
        tokenId = tokenId,
        owner = ownershipId.owner.value,
        balance = BigInteger.ONE
    )
}


fun randomTezosOrderDto() = randomTezosOrderDto(randomTezosAssetNFT(randomTezosItemId()), randomString(), randomTezosAssetXtz())
fun randomTezosOrderDto(itemId: ItemIdDto) = randomTezosOrderDto(itemId, randomString())
fun randomTezosOrderDto(itemId: ItemIdDto, maker: String) = randomTezosOrderDto(
    randomTezosAssetNFT(itemId),
    maker,
    randomTezosAssetXtz()
)

fun randomTezosAssetXtz(): Asset {
    return Asset(
        assetType = Asset.XTZ(),
        assetValue = BigDecimal.ONE
    )
}

fun randomTezosAssetNFT() = randomTezosAssetNFT(randomTezosItemId())
fun randomTezosAssetNFT(itemId: ItemIdDto): Asset {
    val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
    return Asset(
        assetType = Asset.NFT(
            contract = contract,
            tokenId = tokenId
        ),
        assetValue = BigDecimal.ONE
    )
}

fun randomTezosOrderDto(make: Asset, maker: String, take: Asset): DipDupOrder {
    return DipDupOrder(
        id = UUID.randomUUID().toString(),
        fill = BigDecimal.ZERO,
        platform = TezosPlatform.RARIBLE_V2,
        payouts = emptyList(),
        originFees = emptyList(),
        status = OrderStatus.ACTIVE,
        startAt = null,
        endAt = null,
        endedAt = null,
        lastUpdatedAt = nowMillis().atOffset(ZoneOffset.UTC),
        createdAt = nowMillis().atOffset(ZoneOffset.UTC),
        maker = maker,
        makePrice = null,
        make = make,
        taker = null,
        take = take,
        takePrice = null,
        cancelled = false,
        salt = BigInteger.ONE
    )
}

fun randomTezosOrderListActivity(): DipDupActivity {
    return DipDupOrderListActivity(
        id = randomString(),
        date = nowMillis().atOffset(ZoneOffset.UTC),
        reverted = false,
        operationCounter = 1,
        hash = randomString(),
        source = TezosPlatform.RARIBLE_V2,
        maker = randomString(),
        make = Asset(
            assetType = Asset.NFT(
                contract = UUID.randomUUID().toString(),
                tokenId = BigInteger.ONE
            ),
            assetValue = BigDecimal.ONE
        ),
        take = Asset(
            assetType = Asset.XTZ(),
            assetValue = BigDecimal.ONE
        )
    )
}

fun randomTezosOrderActivityCancelList(): DipDupOrderCancelActivity {
    return DipDupOrderCancelActivity(
        id = randomString(),
        date = nowMillis().atOffset(ZoneOffset.UTC),
        reverted = false,
        operationCounter = 1,
        hash = randomString(),
        source = TezosPlatform.RARIBLE_V2,
        maker = randomString(),
        make = Asset(
            assetType = Asset.NFT(
                contract = UUID.randomUUID().toString(),
                tokenId = BigInteger.ONE
            ),
            assetValue = BigDecimal.ONE
        ),
        take = Asset(
            assetType = Asset.XTZ(),
            assetValue = BigDecimal.ONE
        )
    )
}

fun randomTezosItemMintActivity(): DipDupMintActivity {
    return DipDupMintActivity(
        id = randomString(),
        date = nowMillis().atOffset(ZoneOffset.UTC),
        owner = randomString(),
        contract = randomString(),
        tokenId = randomBigInt(),
        value = randomBigInt().toBigDecimal(),
        transactionId = randomString(),
        reverted = false,
        transferId = BigInteger.ONE.toString()
    )
}

fun randomTezosItemBurnActivity(): DipDupBurnActivity {
    return DipDupBurnActivity(
        id = randomString(),
        date = nowMillis().atOffset(ZoneOffset.UTC),
        owner = randomString(),
        contract = randomString(),
        tokenId = randomBigInt(),
        value = randomBigInt().toBigDecimal(),
        transactionId = randomString(),
        reverted = false,
        transferId = BigInteger.ONE.toString()
    )
}

fun randomTezosItemTransferActivity(): DipDupTransferActivity {
    return DipDupTransferActivity(
        id = randomString(),
        date = nowMillis().atOffset(ZoneOffset.UTC),
        owner = randomString(),
        contract = randomString(),
        tokenId = randomBigInt(),
        value = randomBigInt().toBigDecimal(),
        transactionId = randomString(),
        reverted = false,
        transferId = BigInteger.ONE.toString(),
        from = randomString()
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

fun randomTzktToken(itemId: String) = Token(
    id = 1,
    contract = Alias(
        alias = "test name",
        address = "test"
    ),
    tokenId = "123",
    firstTime = nowMillis().atOffset(ZoneOffset.UTC),
    lastTime = nowMillis().atOffset(ZoneOffset.UTC),
    totalSupply = "1",
    transfersCount = 1,
    balancesCount = 1,
    holdersCount = 1
)

fun randomTzktTokenBalance(ownerId: OwnershipIdDto): TokenBalance {
    return TokenBalance(
        id = randomLong(),
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
