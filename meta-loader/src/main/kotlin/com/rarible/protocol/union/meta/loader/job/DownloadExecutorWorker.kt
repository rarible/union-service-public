package com.rarible.protocol.union.meta.loader.job

import com.rarible.core.daemon.sequential.SequentialDaemonWorker

class DownloadExecutorWorker(
    private val enabled: Boolean = true,
    private val workers: List<SequentialDaemonWorker>
) : AutoCloseable {

    fun start() {
        if (enabled) {
            workers.forEach { it.start() }
        }
    }

    override fun close() {
        if (enabled) {
            workers.forEach { it.close() }
        }
    }
}
