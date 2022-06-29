package com.rarible.protocol.union.core.model

data class EsActivityQueryResult(
    val activities: List<EsActivityLite>,
    val cursor: String?,
)
