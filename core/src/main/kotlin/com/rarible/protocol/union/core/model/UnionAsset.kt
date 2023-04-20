package com.rarible.protocol.union.core.model

import java.math.BigDecimal

data class UnionAsset(
    val type: UnionAssetType,
    val value: BigDecimal
)
