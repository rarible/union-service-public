package com.rarible.protocol.union.core.model.elastic

enum class EsSortOrder {
    ASC,
    DESC;

    fun isAsc(): Boolean = this == ASC
}
