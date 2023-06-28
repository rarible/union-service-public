package com.rarible.protocol.union.worker.job

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.atomic.AtomicReference

abstract class AbstractBatchJob {

    fun handle(continuation: String?, param: String): Flow<String> {
        val next = AtomicReference(continuation)
        return flow {
            do {
                next.set(handleBatch(next.get(), param))
                next.get()?.let { emit(it) }
            } while (next.get() != null)
        }
    }

    abstract suspend fun handleBatch(continuation: String?, param: String): String?
}