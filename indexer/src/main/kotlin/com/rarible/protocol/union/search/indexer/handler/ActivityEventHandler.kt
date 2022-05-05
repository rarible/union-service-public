package com.rarible.protocol.union.search.indexer.handler

import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.core.converter.EsActivityConverter
import com.rarible.protocol.union.core.model.elasticsearch.EsEntity
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.search.indexer.metrics.IndexerMetricFactory
import org.springframework.stereotype.Service

@Service
class ActivityEventHandler(
    private val repository: EsActivityRepository,
    metricFactory: IndexerMetricFactory,
): ConsumerBatchEventHandler<ActivityDto> {

    companion object {
        private val logger by Logger()
    }

    private val gauge = metricFactory.createEventHandlerGaugeMetric(EsEntity.ACTIVITY)

    override suspend fun handle(event: List<ActivityDto>) {
        logger.info("Handling ${event.size} ActivityDto events")

        val convertedEvents = event.mapNotNull {
            logger.debug("Converting ActivityDto id = ${it.id}")
            EsActivityConverter.convert(it)
        }

        if (convertedEvents.isNotEmpty()) gauge.set(convertedEvents.last().date.epochSecond)

        logger.debug("Saving ${convertedEvents.size} ActivityDto events to ElasticSearch")
        repository.saveAll(convertedEvents)
        logger.info("Handling completed")
    }
}
