package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.ItemHistoryDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import java.math.BigInteger
import java.time.Instant

data class UnionOwnership(
    val id: OwnershipIdDto,
    val collection: CollectionIdDto?,
    val value: BigInteger,
    val createdAt: Instant,
    val creators: List<CreatorDto> = listOf(),
    val lazyValue: BigInteger,
    val pending: List<ItemHistoryDto> = listOf()
) 