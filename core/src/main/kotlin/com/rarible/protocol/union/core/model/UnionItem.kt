package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemTransferDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.UnionAddress
import java.math.BigInteger
import java.time.Instant

data class UnionItem(
    val id: ItemIdDto,
    val collection: CollectionIdDto?,
    val creators: List<CreatorDto> = listOf(),
    val owners: List<UnionAddress> = listOf(),
    val royalties: List<RoyaltyDto> = listOf(),
    val lazySupply: BigInteger,
    val pending: List<ItemTransferDto> = listOf(),
    val mintedAt: Instant,
    val lastUpdatedAt: Instant,
    val supply: BigInteger,
    val meta: UnionMeta? = null,
    val deleted: Boolean
)
