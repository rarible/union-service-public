package com.rarible.protocol.union.integration.solana.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.solana.protocol.dto.TokenDto
import java.math.BigInteger

fun randomSolanaTokenDto(itemId: ItemIdDto) = TokenDto(
    address = itemId.value,
    supply = randomBigInt(),
    createdAt = nowMillis(),
    updatedAt = nowMillis(),
    closed = false
)

fun randomSolanaTokenAddress() = randomString()

fun randomSolanaItemId() = ItemIdDto(
    blockchain = BlockchainDto.SOLANA,
    contract = randomSolanaTokenAddress(),
    tokenId = BigInteger.ZERO
)
