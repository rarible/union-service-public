package com.rarible.protocol.union.core.model.elastic

data class EsTraitFilter(
    val text: String?,
    val keys: Set<String>,
    val blockchains: Set<String>,
    val collectionIds: Set<String>,
    val listed: Boolean,
    val valueFrequencySortOrder: EsSortOrder,
    val keysLimit: Int,
    val valuesLimit: Int,
)
