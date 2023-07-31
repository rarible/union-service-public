package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.UnionAddress
import java.math.BigDecimal
import java.time.Instant

data class ItemLastSale(
    val date: Instant,
    val seller: UnionAddress,
    val buyer: UnionAddress,
    val value: BigDecimal,
    val currency: AssetTypeDto,
    val price: BigDecimal
)
