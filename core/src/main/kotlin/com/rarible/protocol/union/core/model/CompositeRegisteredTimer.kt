package com.rarible.protocol.union.core.model

import com.rarible.core.telemetry.metrics.RegisteredTimer
import com.rarible.protocol.union.dto.BlockchainDto
import java.time.Duration

class CompositeRegisteredTimer(
    private val timers: Map<BlockchainDto, RegisteredTimer>
) {
    fun record(amount: Duration, blockchainDto: BlockchainDto) {
        getTimer(blockchainDto).record(amount)
    }

    private fun getTimer(blockchainDto: BlockchainDto): RegisteredTimer {
        return timers[blockchainDto] ?: throw IllegalStateException("Can't find delay metric timer for $blockchainDto")
    }
}