package com.rarible.protocol.union.search.indexer.metrics

import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.protocol.union.core.model.elasticsearch.EsEntity
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OrderEventDto
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class MetricConsumerBatchEventHandlerFactory(
    private val metricFactory: IndexerMetricFactory
) {
    fun wrapActivity(handler: ConsumerBatchEventHandler<ActivityDto>): ConsumerBatchEventHandler<ActivityDto> {
        return MetricsConsumerBatchEventHandlerWrapper(
            metricFactory = metricFactory,
            delegate = handler,
            esEntity = EsEntity.ACTIVITY,
            eventTimestamp = { event -> event.date },
            eventBlockchain = { event -> event.id.blockchain }
        )
    }

    fun wrapCollection(handler: ConsumerBatchEventHandler<CollectionEventDto>): ConsumerBatchEventHandler<CollectionEventDto> {
        return MetricsConsumerBatchEventHandlerWrapper(
            metricFactory = metricFactory,
            delegate = handler,
            esEntity = EsEntity.COLLECTION,
            eventTimestamp = { Instant.now() },
            eventBlockchain = { event -> event.collectionId.blockchain }
        )
    }

    fun wrapOrder(handler: ConsumerBatchEventHandler<OrderEventDto>): ConsumerBatchEventHandler<OrderEventDto> {
        return MetricsConsumerBatchEventHandlerWrapper(
            metricFactory = metricFactory,
            delegate = handler,
            esEntity = EsEntity.COLLECTION,
            eventTimestamp = { Instant.now() },
            eventBlockchain = { event -> event.orderId.blockchain }
        )
    }

    fun wrapItem(handler: ConsumerBatchEventHandler<ItemEventDto>): ConsumerBatchEventHandler<ItemEventDto> {
        return MetricsConsumerBatchEventHandlerWrapper(
            metricFactory = metricFactory,
            delegate = handler,
            esEntity = EsEntity.ITEM,
            eventTimestamp = { Instant.now() },
            eventBlockchain = { event -> event.itemId.blockchain }
        )
    }
}