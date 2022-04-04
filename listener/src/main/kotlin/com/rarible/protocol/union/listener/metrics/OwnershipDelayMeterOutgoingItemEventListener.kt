package com.rarible.protocol.union.listener.metrics

import com.rarible.protocol.union.core.model.CompositeRegisteredTimer
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipDeleteEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.OwnershipUpdateEventDto
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

@Component
class OwnershipDelayMeterOutgoingItemEventListener<T>(
    private val clock: Clock,
    ownershipCompositeRegisteredTimer: CompositeRegisteredTimer,
) : EntityDelayMeterOutgoingItemEventListener<OwnershipEventDto>(clock, ownershipCompositeRegisteredTimer) {

    override fun extractLastUpdateAt(event: OwnershipEventDto): Instant {
        return when (event) {
            is OwnershipUpdateEventDto -> event.ownership.createdAt
            is OwnershipDeleteEventDto -> clock.instant()
        }
    }

    override fun extractBlockchain(event: OwnershipEventDto): BlockchainDto {
        return event.ownershipId.blockchain
    }
}