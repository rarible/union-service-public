package com.rarible.protocol.union.core.continuation.page

data class ArgPage<T>(
    val arg: String,
    val argContinuation: String?,
    val page: Page<T>
) {

    companion object {
        const val COMPLETED = "COMPLETED"
    }

    fun isFinished(): Boolean {
        return argContinuation == COMPLETED || (page.continuation == null && page.entities.isEmpty())
    }

    fun hasNext(): Boolean {
        return page.continuation != null
    }

    fun toSlice(): ArgSlice<T> {
        return ArgSlice(arg, argContinuation, Slice(page.continuation, page.entities))
    }

}
