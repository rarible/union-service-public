package com.rarible.protocol.union.enrichment.metrics

import com.rarible.protocol.union.core.model.elasticsearch.EsEntity
import com.rarible.protocol.union.dto.BlockchainDto
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class EsMetricFactory(
    private val meterRegistry: MeterRegistry,
) {
    private companion object {
        const val ROOT_PATH: String = "protocol.union.es"
        const val ENTITY_TAG = "entity"
        const val BLOCKCHAIN_TAG = "blockchain"
    }

    fun createEntitySaveCountMetric(entity: EsEntity, blockchain: BlockchainDto): Counter {
        return Counter.builder("$ROOT_PATH.entity.save")
            .tag(ENTITY_TAG, entity.entityName)
            .tag(BLOCKCHAIN_TAG, blockchain.name.lowercase())
            .register(meterRegistry)
    }
}