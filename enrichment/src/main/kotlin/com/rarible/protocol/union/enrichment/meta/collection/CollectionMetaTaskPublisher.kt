package com.rarible.protocol.union.enrichment.meta.collection

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadTaskPublisher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class CollectionMetaTaskPublisher(
    @Qualifier("download.scheduler.task.producer.collection-meta")
    producer: RaribleKafkaProducer<DownloadTaskEvent>
) : DownloadTaskPublisher(producer)
