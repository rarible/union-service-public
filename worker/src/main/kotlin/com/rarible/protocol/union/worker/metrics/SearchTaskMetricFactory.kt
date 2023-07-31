package com.rarible.protocol.union.worker.metrics

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.protocol.union.core.model.elastic.EsEntity
import com.rarible.protocol.union.core.task.OwnershipTaskParam
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.SyncTypeDto
import com.rarible.protocol.union.worker.config.WorkerProperties
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.springframework.stereotype.Component

@Component
class SearchTaskMetricFactory(
    private val meterRegistry: MeterRegistry,
    private val properties: WorkerProperties
) {
    fun createReindexActivityCounter(
        blockchain: BlockchainDto,
        type: SyncTypeDto
    ): RegisteredCounter {
        return object : CountingMetric(
            name = getReindexEntityMetricName(EsEntity.ACTIVITY),
            Tag.of("blockchain", blockchain.name.lowercase()),
            Tag.of("type", type.name.lowercase())
        ) {}.bind(meterRegistry)
    }

    fun createReindexOwnershipCounter(
        blockchain: BlockchainDto,
        target: OwnershipTaskParam.Target,
    ): RegisteredCounter {
        return object : CountingMetric(
            name = getReindexEntityMetricName(EsEntity.OWNERSHIP),
            Tag.of("blockchain", blockchain.name.lowercase()),
            Tag.of("target", target.name.lowercase())
        ) {}.bind(meterRegistry)
    }

    fun createReindexItemCounter(
        blockchain: BlockchainDto
    ): RegisteredCounter {
        return object : CountingMetric(
            name = getReindexEntityMetricName(EsEntity.ITEM),
            Tag.of("blockchain", blockchain.name.lowercase())
        ) {}.bind(meterRegistry)
    }

    fun createReindexOrderCounter(
        blockchain: BlockchainDto
    ): RegisteredCounter {
        return object : CountingMetric(
            name = getReindexEntityMetricName(EsEntity.ORDER),
            Tag.of("blockchain", blockchain.name.lowercase())
        ) {}.bind(meterRegistry)
    }

    fun createReindexCollectionCounter(
        blockchain: BlockchainDto
    ): RegisteredCounter {
        return object : CountingMetric(
            name = getReindexEntityMetricName(EsEntity.COLLECTION),
            Tag.of("blockchain", blockchain.name.lowercase()),
        ) {}.bind(meterRegistry)
    }

    private fun getReindexEntityMetricName(entity: EsEntity): String {
        return "${properties.metrics.rootPath}.reindex.${entity.name.lowercase()}"
    }
}
