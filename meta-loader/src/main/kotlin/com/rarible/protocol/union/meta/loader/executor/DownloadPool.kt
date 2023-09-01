package com.rarible.protocol.union.meta.loader.executor

import com.rarible.core.common.asyncWithTraceId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class DownloadPool(
    numberOfThreads: Int,
    private val threadPrefix: String
) : AutoCloseable {

    private val daemonDispatcher = Executors.newFixedThreadPool(numberOfThreads) { runnable ->
        Thread(runnable, "$threadPrefix-${THREAD_INDEX.getAndIncrement()}")
            .apply { isDaemon = true }
    }.asCoroutineDispatcher()

    private val scope = CoroutineScope(SupervisorJob() + daemonDispatcher)

    suspend fun submitAsync(block: suspend () -> Unit): Deferred<Unit> {
        return scope.asyncWithTraceId { block() }
    }

    override fun close() {
        scope.cancel()
        daemonDispatcher.close()
    }

    private companion object {

        val THREAD_INDEX = AtomicInteger()
    }
}
