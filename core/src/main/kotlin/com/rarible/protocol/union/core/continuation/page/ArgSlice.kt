package com.rarible.protocol.union.core.continuation.page

data class ArgSlice<T>(
    val arg: String,
    val argContinuation: String?,
    val slice: Slice<T>
) {

    companion object {
        const val COMPLETED = "COMPLETED"
    }

    fun isFinished(): Boolean {
        return argContinuation == COMPLETED || (slice.continuation == null && slice.entities.isEmpty())
    }

    fun hasNext(): Boolean {
        return slice.continuation != null
    }

}