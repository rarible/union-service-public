package com.rarible.protocol.union.core.model

import com.rarible.core.telemetry.metrics.TimingMetric
import com.rarible.protocol.union.dto.BlockchainDto
import io.micrometer.core.instrument.MeterRegistry

abstract class UnionEntityEventDelayMetric(private val name: String) {
    fun bind(registry: MeterRegistry): CompositeRegisteredTimer {
        return CompositeRegisteredTimer(
            BlockchainDto.values().associateWith { blockchain ->
                BlockchainTimingMetric(name, blockchain).bind(registry)
            }
        )
    }

    private class BlockchainTimingMetric(name: String, blockchain: BlockchainDto) : TimingMetric(
        name, emptyList(), tag("blockchain", blockchain.name.lowercase())
    )
}

class ItemEventDelayMetric(root: String) : UnionEntityEventDelayMetric("$root.event.delay.item")

class OwnershipEventDelayMetric(root: String) : UnionEntityEventDelayMetric("$root.event.delay.ownership")

class OrderEventDelayMetric(root: String) : UnionEntityEventDelayMetric("$root.event.delay.order")
