package com.rarible.protocol.union.enrichment.model

import java.math.BigDecimal

/** Класс с информацией по аттрибуту (трейту) айтема и его редкости. */
data class ItemAttributeWithRarity(
    val key: String,
    val value: String,
    val rarity: BigDecimal
)
