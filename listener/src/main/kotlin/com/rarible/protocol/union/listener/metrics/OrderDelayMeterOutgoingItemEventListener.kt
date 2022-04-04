package com.rarible.protocol.union.listener.metrics

import com.rarible.protocol.union.core.model.CompositeRegisteredTimer
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OrderUpdateEventDto
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

@Component
class OrderDelayMeterOutgoingItemEventListener<T>(
    clock: Clock,
    orderCompositeRegisteredTimer: CompositeRegisteredTimer,
) : EntityDelayMeterOutgoingItemEventListener<OrderEventDto>(clock, orderCompositeRegisteredTimer) {

    override fun extractLastUpdateAt(event: OrderEventDto): Instant {
        return when (event) {
            is OrderUpdateEventDto -> event.order.lastUpdatedAt
        }
    }

    override fun extractBlockchain(event: OrderEventDto): BlockchainDto {
        return event.orderId.blockchain
    }
}