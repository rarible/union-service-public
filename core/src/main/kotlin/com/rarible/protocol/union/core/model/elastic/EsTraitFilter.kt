package com.rarible.protocol.union.core.model.elastic

data class EsTraitFilter(
    val text: String? = null,
    val keys: Set<String> = emptySet(),
    val collectionIds: Set<String> = emptySet(),
    val valueFrequencySortOrder: EsSortOrder = EsSortOrder.DESC,
    val listed: Boolean = false,
    val keysLimit: Int,
    val valuesLimit: Int,
)
