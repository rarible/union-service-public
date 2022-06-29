package com.rarible.protocol.union.core.model

data class EsQueryResult<T>(
    val content: List<T>,
    val cursor: String?,
    val total: Long? = null
)
