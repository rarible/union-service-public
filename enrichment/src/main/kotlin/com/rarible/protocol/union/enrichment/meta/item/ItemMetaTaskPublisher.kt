package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.model.download.DownloadTask
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadTaskPublisher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class ItemMetaTaskPublisher(
    @Qualifier("download.scheduler.task.producer.item-meta")
    producer: RaribleKafkaProducer<DownloadTask>
) : DownloadTaskPublisher(producer)
