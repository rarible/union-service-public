package com.rarible.protocol.union.search.indexer.metrics

import com.rarible.core.common.nowMillis
import com.rarible.core.kafka.RaribleKafkaBatchEventHandler
import com.rarible.protocol.union.core.event.EventCountMetrics
import com.rarible.protocol.union.core.model.elastic.EsEntity
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import org.springframework.stereotype.Component

@Component
class MetricConsumerBatchEventHandlerFactory(
    private val metricFactory: IndexerMetricFactory,
    private val eventCountMetrics: EventCountMetrics
) {
    fun wrapActivity(handler: RaribleKafkaBatchEventHandler<ActivityDto>): RaribleKafkaBatchEventHandler<ActivityDto> {
        return MetricsConsumerBatchEventHandlerWrapper(
            metricFactory = metricFactory,
            eventCountMetrics = eventCountMetrics,
            delegate = handler,
            esEntity = EsEntity.ACTIVITY,
            eventTimestamp = { event -> event.date },
            eventBlockchain = { event -> event.id.blockchain }
        )
    }

    fun wrapCollection(handler: RaribleKafkaBatchEventHandler<CollectionEventDto>): RaribleKafkaBatchEventHandler<CollectionEventDto> {
        return MetricsConsumerBatchEventHandlerWrapper(
            metricFactory = metricFactory,
            eventCountMetrics = eventCountMetrics,
            delegate = handler,
            esEntity = EsEntity.COLLECTION,
            eventTimestamp = { nowMillis() },
            eventBlockchain = { event -> event.collectionId.blockchain }
        )
    }

    fun wrapOrder(handler: RaribleKafkaBatchEventHandler<OrderEventDto>): RaribleKafkaBatchEventHandler<OrderEventDto> {
        return MetricsConsumerBatchEventHandlerWrapper(
            metricFactory = metricFactory,
            eventCountMetrics = eventCountMetrics,
            delegate = handler,
            esEntity = EsEntity.ORDER,
            eventTimestamp = { nowMillis() },
            eventBlockchain = { event -> event.orderId.blockchain }
        )
    }

    fun wrapItem(handler: RaribleKafkaBatchEventHandler<ItemEventDto>): RaribleKafkaBatchEventHandler<ItemEventDto> {
        return MetricsConsumerBatchEventHandlerWrapper(
            metricFactory = metricFactory,
            eventCountMetrics = eventCountMetrics,
            delegate = handler,
            esEntity = EsEntity.ITEM,
            eventTimestamp = { nowMillis() },
            eventBlockchain = { event -> event.itemId.blockchain }
        )
    }

    fun wrapOwnership(handler: RaribleKafkaBatchEventHandler<OwnershipEventDto>): RaribleKafkaBatchEventHandler<OwnershipEventDto> {
        return MetricsConsumerBatchEventHandlerWrapper(
            metricFactory = metricFactory,
            eventCountMetrics = eventCountMetrics,
            delegate = handler,
            esEntity = EsEntity.OWNERSHIP,
            eventTimestamp = { nowMillis() },
            eventBlockchain = { event -> event.ownershipId.blockchain }
        )
    }
}
