package com.rarible.protocol.union.api.metrics

import com.rarible.protocol.union.core.model.elasticsearch.EsEntity
import com.rarible.protocol.union.dto.BlockchainDto
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.ImmutableTag
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class ElasticMetricsFactory(
    private val meterRegistry: MeterRegistry,
) {
    companion object {
        private const val rootPath: String = "protocol.union.api"
    }

    val missingActivitiesCounters = BlockchainDto.values().associateWith {
        createMissingEntitiesCounter(EsEntity.ACTIVITY, it)
    }

    private fun createMissingEntitiesCounter(
        entity: EsEntity,
        blockchain: BlockchainDto,
    ): Counter {
        return meterRegistry.counter(
            "$rootPath.es.entities.missing",
            listOf(
                ImmutableTag("blockchain", blockchain.name.lowercase()),
                ImmutableTag("entity", entity.name.lowercase())
            )
        )
    }
}
