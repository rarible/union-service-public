package com.rarible.protocol.union.search.indexer.metrics

import com.rarible.core.common.nowMillis
import com.rarible.core.kafka.RaribleKafkaBatchEventHandler
import com.rarible.protocol.union.core.event.EventCountMetrics
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.model.elastic.EsEntity
import com.rarible.protocol.union.dto.BlockchainDto
import java.time.Instant

class MetricsConsumerBatchEventHandlerWrapper<T>(
    metricFactory: IndexerMetricFactory,
    private val eventCountMetrics: EventCountMetrics,
    private val delegate: RaribleKafkaBatchEventHandler<T>,
    private val esEntity: EsEntity,
    private val eventTimestamp: (T) -> Instant,
    private val eventBlockchain: (T) -> BlockchainDto
) : RaribleKafkaBatchEventHandler<T> {

    private val counters = BlockchainDto.values().associateWith {
        metricFactory.createEventHandlerCountMetric(esEntity, it)
    }

    private val delayGauges = BlockchainDto.values().associateWith {
        metricFactory.createEventHandlerDelayMetric(esEntity, it)
    }

    override suspend fun handle(events: List<T>) {
        val eventsByBlockchain = events.groupBy { eventBlockchain(it) }
        try {
            recordCount(eventsByBlockchain)
            delegate.handle(events)
        } catch (e: Throwable) {
            rollbackCount(eventsByBlockchain)
            throw e
        }
        recordMetrics(eventsByBlockchain)
    }

    private fun recordCount(events: Map<BlockchainDto, List<T>>) {
        events.forEach { (blockchain, events) ->
            eventCountMetrics.eventReceived(
                EventCountMetrics.Stage.EXTERNAL,
                blockchain,
                EventType.valueOf(esEntity.name),
                events.size
            )
        }
    }

    private fun rollbackCount(events: Map<BlockchainDto, List<T>>) {
        events.forEach { (blockchain, events) ->
            eventCountMetrics.eventReceived(
                EventCountMetrics.Stage.EXTERNAL,
                blockchain,
                EventType.valueOf(esEntity.name),
                -events.size
            )
        }
    }

    private fun recordMetrics(events: Map<BlockchainDto, List<T>>) {
        events.forEach { (blockchain, events) ->
            val now = nowMillis().epochSecond
            val last = eventTimestamp(events.last()).epochSecond
            requireNotNull(delayGauges[blockchain]).set(now - last)
            requireNotNull(counters[blockchain]).increment(events.size.toDouble())
        }
    }
}
