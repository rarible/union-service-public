package com.rarible.protocol.union.core.model.elastic

data class EsActivityQueryResult(
    val activities: List<EsActivity>,
    val cursor: String?,
)
