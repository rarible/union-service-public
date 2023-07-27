package com.rarible.protocol.union.listener.downloader

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.download.DownloadTask
import kotlinx.coroutines.flow.collect

class MetaTaskRouter(
    private val pipelineSenders: Map<String, RaribleKafkaProducer<DownloadTask>>
) : DownloadTaskRouter, AutoCloseable {

    override suspend fun send(tasks: List<DownloadTask>, pipeline: String) {
        val sender = pipelineSenders[pipeline]
            ?: throw IllegalArgumentException("Can't find sender for pipeline: $pipeline")

        val messages = tasks.map { KafkaEventFactory.downloadTaskEvent(it) }
        sender.send(messages).collect()
    }

    override fun close() {
        pipelineSenders.values.forEach { it.close() }
    }
}
