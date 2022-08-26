package com.rarible.protocol.union.meta.loader.executor

// We need to use such centralized executor as bean in order to make it possible
// to close all the download pools gracefully on app shutdown (since DownloadExecutors are not beans)
class DownloadExecutorManager(
    private val executors: Map<String, DownloadExecutor<*>>
) : AutoCloseable {

    fun getExecutor(pipeline: String) = executors[pipeline]
        ?: throw IllegalArgumentException("Download executor for the pipeline $pipeline is not found")

    override fun close() {
        executors.values.forEach { it.close() }
    }

}