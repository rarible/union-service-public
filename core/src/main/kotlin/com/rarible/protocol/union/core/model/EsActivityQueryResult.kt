package com.rarible.protocol.union.core.model

data class EsActivityQueryResult(
    val activities: List<EsActivityInfo>,
    val cursor: String?,
)
