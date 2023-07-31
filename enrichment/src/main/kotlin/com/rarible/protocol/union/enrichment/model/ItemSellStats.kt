package com.rarible.protocol.union.enrichment.model

import java.math.BigInteger

data class ItemSellStats(
    val sellers: Int,
    val totalStock: BigInteger
)
