package com.rarible.protocol.union.enrichment.meta.downloader

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import com.rarible.protocol.union.enrichment.event.EnrichmentKafkaEventFactory
import kotlinx.coroutines.flow.collect

/**
 * Publisher of the tasks to the scheduler
 */
abstract class DownloadTaskPublisher(
    private val producer: RaribleKafkaProducer<DownloadTaskEvent>
) {

    suspend fun publish(tasks: List<DownloadTaskEvent>) {
        producer.send(tasks.map { EnrichmentKafkaEventFactory.downloadTaskEvent(it) }).collect()
    }
}
