package com.rarible.protocol.union.listener.downloader

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import com.rarible.protocol.union.enrichment.event.EnrichmentKafkaEventFactory
import kotlinx.coroutines.flow.collect

class MetaTaskRouter(
    private val pipelineSenders: Map<String, RaribleKafkaProducer<DownloadTaskEvent>>
) : DownloadTaskRouter, AutoCloseable {

    override suspend fun send(tasks: List<DownloadTaskEvent>, pipeline: String) {
        val sender = pipelineSenders[pipeline]
            ?: throw IllegalArgumentException("Can't find sender for pipeline: $pipeline")

        val messages = tasks.map { EnrichmentKafkaEventFactory.downloadTaskEvent(it) }
        sender.send(messages).collect()
    }

    override fun close() {
        pipelineSenders.values.forEach { it.close() }
    }
}
