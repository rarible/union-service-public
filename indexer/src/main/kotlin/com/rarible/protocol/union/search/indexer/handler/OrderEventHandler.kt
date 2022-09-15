package com.rarible.protocol.union.search.indexer.handler

import com.rarible.core.common.nowMillis
import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.converter.EsOrderConverter
import com.rarible.protocol.union.core.model.EsOrder
import com.rarible.protocol.union.core.model.elasticsearch.EsEntity
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.enrichment.repository.search.EsOrderRepository
import com.rarible.protocol.union.search.indexer.metrics.IndexerMetricFactory
import org.elasticsearch.action.support.WriteRequest
import org.springframework.stereotype.Service

@Service
class OrderEventHandler(
    private val featureFlagsProperties: FeatureFlagsProperties,
    private val repository: EsOrderRepository,
    metricFactory: IndexerMetricFactory,
) : ConsumerBatchEventHandler<OrderEventDto> {

    private val logger by Logger()

    private val entitySaveCounters = BlockchainDto.values().associateWith {
        metricFactory.createEntitySaveCountMetric(EsEntity.ORDER, it)
    }

    override suspend fun handle(event: List<OrderEventDto>) {
        val startTime = nowMillis()
        logger.info("Handling ${event.size} OrderDto events")

        val convertedEvents = event.map {
            logger.info("Converting OrderDto  $it")
            EsOrderConverter.convert(it)
        }
        if (convertedEvents.isNotEmpty()) {
            logger.info("Saving ${convertedEvents.size} OrderDto events to ElasticSearch")
            val refreshPolicy =
                if (featureFlagsProperties.enableOrderSaveImmediateToElasticSearch) {
                    WriteRequest.RefreshPolicy.IMMEDIATE
                } else {
                    WriteRequest.RefreshPolicy.NONE
                }
            repository.bulk(convertedEvents, idsToDelete = emptyList(), refreshPolicy = refreshPolicy)
            countSaves(convertedEvents)

            val elapsedTime = nowMillis().minusMillis(startTime.toEpochMilli()).toEpochMilli()
            logger.info("Handling of ${event.size} OrderDto events completed in $elapsedTime ms")
        }
    }

    private fun countSaves(orders: List<EsOrder>) {
        val countByBlockchain = orders.groupingBy { it.blockchain }.eachCount()
        countByBlockchain.entries.forEach {
            entitySaveCounters[it.key]!!.increment(it.value.toDouble())
        }
    }
}
