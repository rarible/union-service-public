package com.rarible.protocol.union.core.model

import java.math.BigDecimal

data class UnionAssetDto(
    val type: UnionAssetTypeDto,
    val value: BigDecimal
)
