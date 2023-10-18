package com.rarible.protocol.union.search.indexer.metrics

import com.rarible.core.telemetry.metrics.LongGaugeMetric
import com.rarible.core.telemetry.metrics.RegisteredGauge
import com.rarible.protocol.union.core.model.elastic.EsEntity
import com.rarible.protocol.union.dto.BlockchainDto
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class IndexerMetricFactory(
    private val meterRegistry: MeterRegistry
) {

    private val root = "protocol.union.indexer"

    fun createEventHandlerDelayMetric(entity: EsEntity, blockchain: BlockchainDto): RegisteredGauge<Long> {
        return object : LongGaugeMetric(
            name = "$root.event.delay",
            tag(ENTITY_TAG, entity.entityName),
            tag(BLOCKCHAIN_TAG, blockchain.name.lowercase())
        ) {}.bind(meterRegistry)
    }

    fun createEventHandlerCountMetric(entity: EsEntity, blockchain: BlockchainDto): Counter {
        return Counter.builder("$root.event.income")
            .tag(ENTITY_TAG, entity.entityName)
            .tag(BLOCKCHAIN_TAG, blockchain.name.lowercase())
            .register(meterRegistry)
    }

    fun createEntitySaveCountMetric(entity: EsEntity, blockchain: BlockchainDto): Counter {
        return Counter.builder("$root.entity.save")
            .tag(ENTITY_TAG, entity.entityName)
            .tag(BLOCKCHAIN_TAG, blockchain.name.lowercase())
            .register(meterRegistry)
    }

    private companion object {
        const val ENTITY_TAG = "entity"
        const val BLOCKCHAIN_TAG = "blockchain"
    }
}
