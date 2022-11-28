package com.rarible.protocol.union.integration.solana.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBoolean
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.solana.dto.ActivityBlockchainInfoDto
import com.rarible.protocol.solana.dto.AssetDto
import com.rarible.protocol.solana.dto.BalanceDto
import com.rarible.protocol.solana.dto.CollectionDto
import com.rarible.protocol.solana.dto.MintActivityDto
import com.rarible.protocol.solana.dto.OrderBidActivityDto
import com.rarible.protocol.solana.dto.SolanaSolAssetTypeDto
import com.rarible.protocol.solana.dto.TokenCreatorPartDto
import com.rarible.protocol.solana.dto.TokenDto
import com.rarible.protocol.solana.dto.TokenMetaAttributeDto
import com.rarible.protocol.solana.dto.TokenMetaContentDto
import com.rarible.protocol.solana.dto.TokenMetaDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import java.math.BigDecimal
import java.math.BigInteger

fun randomSolanaTokenDto(itemId: ItemIdDto) = TokenDto(
    address = itemId.value,
    supply = randomBigInt(),
    createdAt = nowMillis(),
    updatedAt = nowMillis(),
    creators = listOf(randomTokenCreatorPartDto()),
    collection = randomString(),
    decimals = randomInt(),
    closed = false
)

fun randomTokenCreatorPartDto() = TokenCreatorPartDto(
    address = randomString(),
    share = randomInt()
)

fun randomTokenMeta() = TokenMetaDto(
    name = randomString(),
    description = randomString(),
    creators = (1..(randomInt(1, 3))).map {
        randomTokenCreatorPartDto()
    },
    attributes = (1..(randomInt(1, 3))).map {
        randomTokenMetaAttributeDto()
    },
    content = (1..(randomInt(1, 3))).map {
        randomTokenMetaContentDto()
    },
    status = TokenMetaDto.Status.OK
)

fun randomUrl(): String =
    "https://image.com/${randomString()}"

private fun randomTokenMetaAttributeDto() = TokenMetaAttributeDto(
    key = randomString(),
    value = randomString(),
    type = randomString(),
    format = randomString()
)

private fun randomTokenMetaContentDto(): TokenMetaContentDto = if (randomBoolean()) {
    com.rarible.protocol.solana.dto.ImageContentDto(
        url = randomUrl(),
        representation = TokenMetaContentDto.Representation.values().random(),
        mimeType = randomString(),
        size = randomLong(),
        width = randomInt(),
        height = randomInt(),
    )
} else {
    com.rarible.protocol.solana.dto.VideoContentDto(
        url = randomUrl(),
        representation = TokenMetaContentDto.Representation.values().random(),
        mimeType = randomString(),
        size = randomLong(),
        width = randomInt(),
        height = randomInt(),
    )
}

fun randomSolanaBalanceDto(
    isAssociatedTokenAccount: Boolean = randomBoolean()
) = BalanceDto(
    account = randomString(),
    owner = randomString(),
    mint = randomString(),
    isAssociatedTokenAccount = isAssociatedTokenAccount,
    createdAt = nowMillis(),
    updatedAt = nowMillis(),
    value = randomBigInt()
)

fun randomSolanaTokenAddress() = randomString()

fun randomSolanaItemId() = ItemIdDto(
    blockchain = BlockchainDto.SOLANA,
    contract = randomSolanaTokenAddress(),
    tokenId = BigInteger.ZERO
)

fun randomSolanaMintActivity() = MintActivityDto(
    id = randomString(),
    date = nowMillis(),
    reverted = false,
    owner = randomString(),
    tokenAddress = randomSolanaTokenAddress(),
    value = randomBigInt(),
    blockchainInfo = randomActivityBlockchainInfoDto()
)

fun randomActivityBlockchainInfoDto() = ActivityBlockchainInfoDto(
    blockNumber = randomLong(),
    blockHash = randomString(),
    transactionIndex = randomInt(),
    transactionHash = randomString(),
    instructionIndex = randomInt(),
    innerInstructionIndex = randomInt(),
)

fun randomSolanaCollectionDto() = CollectionDto(
    address = randomSolanaTokenAddress(),
    parent = randomString(),
    name = randomString(),
    symbol = randomString(),
    owner = randomString(),
    features = listOf(
        CollectionDto.Features.values().random()
    ),
    creators = listOf(randomString()),
    meta = null,
)

fun randomActivityOrderBid() = OrderBidActivityDto(
    auctionHouse = null,
    blockchainInfo = randomActivityBlockchainInfoDto(),
    date = nowMillis(),
    dbUpdatedAt = nowMillis(),
    hash = randomString(),
    id = randomString(),
    make = randomAsset(),
    maker = randomString(),
    price = randomBigDecimal(),
    reverted = false,
    take = randomAsset()
)

fun randomAsset() = AssetDto(
    type = SolanaSolAssetTypeDto(),
    value = randomBigDecimal()
)

