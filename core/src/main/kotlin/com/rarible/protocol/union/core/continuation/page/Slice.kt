package com.rarible.protocol.union.core.continuation.page

data class Slice<T>(
    val continuation: String?,
    val entities: List<T>
) {

    companion object {

        fun <T> empty() = Slice<T>(null, emptyList<T>())
    }
}

