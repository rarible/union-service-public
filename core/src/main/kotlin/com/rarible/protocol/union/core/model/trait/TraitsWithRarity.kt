package com.rarible.protocol.union.core.model.trait

import java.math.BigDecimal

data class ExtendedTrait(
    val key: TraitEntryWithRarity,
    val values: List<TraitEntryWithRarity>
)

data class TraitEntryWithRarity(
    val entry: TraitEntry,
    val rarity: BigDecimal
)
