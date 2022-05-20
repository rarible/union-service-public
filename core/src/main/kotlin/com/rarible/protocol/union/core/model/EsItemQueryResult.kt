package com.rarible.protocol.union.core.model

data class EsItemQueryResult(
    val items: List<EsItem>,
    val cursor: String?,
    val total: Long? = null
)
