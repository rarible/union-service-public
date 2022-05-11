package com.rarible.protocol.union.search.indexer.handler

import com.rarible.core.common.nowMillis
import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.core.converter.EsActivityConverter
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.elasticsearch.EsEntity
import com.rarible.protocol.union.dto.BlockchainDto
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

    private val counters = BlockchainDto.values().associateWith {
        metricFactory.createEventHandlerCountMetric(EsEntity.ACTIVITY, it)
    }

    private val delayGauges = BlockchainDto.values().associateWith {
        metricFactory.createEventHandlerDelayMetric(EsEntity.ACTIVITY, it)
    }

    override suspend fun handle(event: List<ActivityDto>) {
        logger.info("Handling ${event.size} ActivityDto events")

        val eventsByBlockchain = mutableMapOf<BlockchainDto, MutableList<EsActivity>>()

        val convertedEvents = event.mapNotNull {
            logger.debug("Converting ActivityDto id = ${it.id}")
            val converted = EsActivityConverter.convert(it)
            if (converted != null) {
                eventsByBlockchain.computeIfAbsent(converted.blockchain) { ArrayList() }
                    .add(converted)
            }
            converted
        }

        eventsByBlockchain.values.forEach {
            recordCount(it)
            recordDelay(it.last())
        }

        logger.debug("Saving ${convertedEvents.size} ActivityDto events to ElasticSearch")
        repository.saveAll(convertedEvents)
        logger.info("Handling completed")
    }

    private fun recordCount(events: List<EsActivity>) {
        counters[events.first().blockchain]!!.increment(events.size.toDouble())
    }

    private fun recordDelay(event: EsActivity) {
        val now = nowMillis().epochSecond
        val last = event.date.epochSecond
        delayGauges[event.blockchain]!!.set(now - last)
    }
}
