package com.rarible.protocol.union.enrichment.meta.downloader

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.download.DownloadTask
import kotlinx.coroutines.flow.collect

/**
 * Publisher of the tasks to the scheduler
 */
abstract class DownloadTaskPublisher(
    private val producer: RaribleKafkaProducer<DownloadTask>
) {

    suspend fun publish(tasks: List<DownloadTask>) {
        producer.send(tasks.map { KafkaEventFactory.downloadTaskEvent(it) }).collect()
    }
}
