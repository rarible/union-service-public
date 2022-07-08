package com.rarible.protocol.union.core.model.trait

data class Trait(
    val key: TraitEntry,
    val values: List<TraitEntry>,
)

data class TraitEntry(
    val value: String,
    val count: Long
)
