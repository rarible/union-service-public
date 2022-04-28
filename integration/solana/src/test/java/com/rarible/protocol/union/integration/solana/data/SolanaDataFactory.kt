package com.rarible.protocol.union.integration.solana.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBoolean
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.solana.dto.ActivityBlockchainInfoDto
import com.rarible.protocol.solana.dto.ActivityDto
import com.rarible.protocol.solana.dto.BalanceDto
import com.rarible.protocol.solana.dto.MintActivityDto
import com.rarible.protocol.solana.dto.TokenCreatorPartDto
import com.rarible.protocol.solana.dto.TokenDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import java.math.BigInteger
import java.time.Instant

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
    date = Instant.now(),
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
