package com.rarible.protocol.union.core.continuation.page

data class Page<T>(
    val total: Long,
    val continuation: String?,
    val entities: List<T>
) {
    companion object {
        fun <T> empty() = Page<T>(0, null, emptyList<T>())
    }
}