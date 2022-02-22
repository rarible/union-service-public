package com.rarible.protocol.union.integration.solana.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBoolean
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.solana.protocol.dto.BalanceDto
import com.rarible.solana.protocol.dto.JsonCollectionDto
import com.rarible.solana.protocol.dto.OnChainCollectionDto
import com.rarible.solana.protocol.dto.TokenCreatorPartDto
import com.rarible.solana.protocol.dto.TokenDto
import java.math.BigInteger

fun randomSolanaTokenDto(itemId: ItemIdDto) = TokenDto(
    address = itemId.value,
    supply = randomBigInt(),
    createdAt = nowMillis(),
    updatedAt = nowMillis(),
    creators = listOf(randomTokenCreatorPartDto()),
    collection = randomCollectionDto(),
    closed = false
)

fun randomTokenCreatorPartDto() = TokenCreatorPartDto(
    address = randomString(),
    share = randomInt()
)

fun randomCollectionDto() = if (randomBoolean()) {
    JsonCollectionDto(
        name = randomString(),
        family = randomString(),
        hash = randomString()
    )
} else {
    OnChainCollectionDto(
        address = randomString(),
        verified = randomBoolean()
    )
}

fun randomSolanaBalanceDto() = BalanceDto(
    account = randomString(),
    owner = randomString(),
    mint = randomString(),
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
