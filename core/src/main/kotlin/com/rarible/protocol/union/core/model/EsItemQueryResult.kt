package com.rarible.protocol.union.core.model

data class EsItemQueryResult(
    val items: List<EsItem>,
    val continuation: String?,
)
