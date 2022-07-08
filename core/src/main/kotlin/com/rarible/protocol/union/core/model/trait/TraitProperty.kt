package com.rarible.protocol.union.core.model.trait

import java.math.BigDecimal

data class TraitProperty(
    val key: String,
    val value: String
)

data class ExtendedTraitProperty(
    val key: String,
    val value: String,
    val rarity: BigDecimal
)
