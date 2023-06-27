package com.rarible.protocol.union.search.indexer.metrics

import com.rarible.core.common.nowMillis
import com.rarible.core.kafka.RaribleKafkaBatchEventHandler
import com.rarible.protocol.union.core.model.elastic.EsEntity
import com.rarible.protocol.union.dto.BlockchainDto
import java.time.Instant

class MetricsConsumerBatchEventHandlerWrapper<T>(
    metricFactory: IndexerMetricFactory,
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

    override suspend fun handle(event: List<T>) {
        delegate.handle(event)

        val eventsByBlockchain = event.groupBy { eventBlockchain(it) }
        eventsByBlockchain.values.forEach {
            recordCount(it)
            recordDelay(it.last())
        }
    }

    private fun recordCount(events: List<T>) {
        val blockchain = eventBlockchain(events.first())
        requireNotNull(counters[blockchain]).increment(events.size.toDouble())
    }

    private fun recordDelay(event: T) {
        val now = nowMillis().epochSecond
        val last = eventTimestamp(event).epochSecond
        val blockchain = eventBlockchain(event)
        requireNotNull(delayGauges[blockchain]).set(now - last)
    }
}
