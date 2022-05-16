package com.rarible.protocol.union.search.indexer.metrics

import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.protocol.union.core.model.elasticsearch.EsEntity
import com.rarible.protocol.union.dto.ActivityDto
import org.springframework.stereotype.Component

@Component
class MetricConsumerBatchEventHandlerFactory(
    private val metricFactory: IndexerMetricFactory
) {
    fun wrap(handler: ConsumerBatchEventHandler<ActivityDto>): ConsumerBatchEventHandler<ActivityDto> {
        return MetricsConsumerBatchEventHandlerWrapper(
            metricFactory = metricFactory,
            delegate = handler,
            esEntity = EsEntity.ACTIVITY,
            eventTimestamp = { event -> event.date },
            eventBlockchain = { event -> event.id.blockchain }
        )
    }
}