package com.rarible.protocol.union.listener.metrics

import com.rarible.protocol.union.core.model.CompositeRegisteredTimer
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemDeleteEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.ItemUpdateEventDto
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

@Component
class ItemDelayMeterOutgoingItemEventListener<T>(
    private val clock: Clock,
    itemCompositeRegisteredTimer: CompositeRegisteredTimer,
) : EntityDelayMeterOutgoingItemEventListener<ItemEventDto>(clock, itemCompositeRegisteredTimer) {

    override fun extractLastUpdateAt(event: ItemEventDto): Instant {
        return when (event) {
            is ItemUpdateEventDto -> event.item.lastUpdatedAt
            is ItemDeleteEventDto -> clock.instant()
        }
    }

    override fun extractBlockchain(event: ItemEventDto): BlockchainDto {
        return event.itemId.blockchain
    }
}