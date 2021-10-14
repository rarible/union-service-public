package com.rarible.protocol.union.dto

import java.math.BigInteger
import java.time.Instant

data class UnionItemDto(
    val id: ItemIdDto,
    val tokenId: BigInteger,
    val collection: UnionAddress,
    val creators: List<CreatorDto> = listOf(),
    val owners: List<UnionAddress> = listOf(),
    val royalties: List<RoyaltyDto> = listOf(),
    val lazySupply: BigInteger,
    val pending: List<ItemTransferDto> = listOf(),
    val mintedAt: Instant,
    val lastUpdatedAt: Instant,
    val supply: BigInteger,
    val meta: MetaDto? = null, // It's better to introduce an intermediate model class for meta and not use final DTO.
    val deleted: Boolean
)
