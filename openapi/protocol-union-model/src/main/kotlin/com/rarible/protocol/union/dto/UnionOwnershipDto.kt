package com.rarible.protocol.union.dto

import java.math.BigInteger
import java.time.Instant

data class UnionOwnershipDto(
    val id: OwnershipIdDto,
    val value: BigInteger,
    val createdAt: Instant,
    val contract: UnionAddress,
    val tokenId: BigInteger,
    val owner: UnionAddress,
    val creators: List<CreatorDto> = listOf(),
    val lazyValue: BigInteger,
    val pending: List<ItemHistoryDto> = listOf()
) 