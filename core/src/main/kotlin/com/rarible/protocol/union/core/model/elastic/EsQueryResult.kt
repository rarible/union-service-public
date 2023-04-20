package com.rarible.protocol.union.core.model.elastic

data class EsQueryResult<T>(
    val content: List<T>,
    val cursor: String?,
    val total: Long? = null
)
