package com.rarible.protocol.union.listener.metrics

import com.rarible.protocol.union.core.event.EventCountMetrics
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.event.OutgoingEventListener
import com.rarible.protocol.union.core.model.ActivityEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.EventTimeMarksDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration

sealed class OutgoingEventMetricsListener<T>(
    private val delays: OutgoingEventDelayMetrics,
    private val metrics: EventCountMetrics
) : OutgoingEventListener<T> {

    private val measuredSources = setOf(
        "blockchain",
        "offchain",
        "integration"
    )

    private val logger = LoggerFactory.getLogger(javaClass)

    abstract val eventType: EventType
    abstract fun getBlockchain(event: T): BlockchainDto
    abstract fun getEventTimeMarks(event: T): EventTimeMarksDto?

    override suspend fun onEvent(event: T) {
        try {
            markDelay(event)
        } catch (e: Exception) {
            logger.error("Failed to mark event {}", event, e)
        }
    }

    private fun markDelay(event: T) {
        val blockchain = getBlockchain(event)
        val eventTimeMarks = getEventTimeMarks(event)
        val marks = eventTimeMarks?.marks
        if (marks.isNullOrEmpty()) {
            // TODO remove later, just an indicator that we have marked all incoming events
            logger.warn("{} event from {} has no time marks", eventType, blockchain)
            return
        }
        if (eventTimeMarks.source !in measuredSources) {
            return
        }
        if (marks.size < 2) {
            // Any event should have at least 2 marks - when it triggered and when it leaves enrichment
            logger.warn("{} event from {} has only one time mark: {}", eventType, blockchain, eventTimeMarks.marks)
            return
        }

        eventTimeMarks.marks.reduce { m1, m2 ->
            val duration = Duration.between(m1.date, m2.date)
            if (duration.isNegative) {
                logger.warn("Negative duration found in {} event from {}: {} -> {}", eventType, blockchain, m1, m2)
            }
            delays.markStageDelay(
                blockchain = blockchain,
                type = eventType,
                source = eventTimeMarks.source,
                from = m1.name,
                to = m2.name,
                delay = Duration.between(m1.date, m2.date)
            )
            m2
        }

        val start = marks.first()
        val end = marks.last()

        delays.markGlobalDelay(
            blockchain = blockchain,
            type = eventType,
            source = eventTimeMarks.source,
            from = start.name,
            to = end.name,
            delay = Duration.between(start.date, end.date)
        )
    }
}

@Component
class CollectionEventMetricsListener(delays: OutgoingEventDelayMetrics, metrics: EventCountMetrics) :
    OutgoingEventMetricsListener<CollectionEventDto>(delays, metrics) {

    override val eventType = EventType.COLLECTION
    override fun getBlockchain(event: CollectionEventDto) = event.collectionId.blockchain
    override fun getEventTimeMarks(event: CollectionEventDto) = event.eventTimeMarks
}

@Component
class ItemEventMetricsListener(delays: OutgoingEventDelayMetrics, metrics: EventCountMetrics) :
    OutgoingEventMetricsListener<ItemEventDto>(delays, metrics) {

    override val eventType = EventType.ITEM
    override fun getBlockchain(event: ItemEventDto) = event.itemId.blockchain
    override fun getEventTimeMarks(event: ItemEventDto) = event.eventTimeMarks
}

@Component
class ActivityMetricsListener(delays: OutgoingEventDelayMetrics, metrics: EventCountMetrics) :
    OutgoingEventMetricsListener<ActivityEvent>(delays, metrics) {

    override val eventType = EventType.ACTIVITY
    override fun getBlockchain(event: ActivityEvent) = event.activity.id.blockchain
    override fun getEventTimeMarks(event: ActivityEvent) = event.eventTimeMarks
}

@Component
class OwnershipEventMetricsListener(delays: OutgoingEventDelayMetrics, metrics: EventCountMetrics) :
    OutgoingEventMetricsListener<OwnershipEventDto>(delays, metrics) {

    override val eventType = EventType.OWNERSHIP
    override fun getBlockchain(event: OwnershipEventDto) = event.ownershipId.blockchain
    override fun getEventTimeMarks(event: OwnershipEventDto) = event.eventTimeMarks
}

@Component
class OrderEventMetricsListener(delays: OutgoingEventDelayMetrics, metrics: EventCountMetrics) :
    OutgoingEventMetricsListener<OrderEventDto>(delays, metrics) {

    override val eventType = EventType.ORDER
    override fun getBlockchain(event: OrderEventDto) = event.orderId.blockchain
    override fun getEventTimeMarks(event: OrderEventDto) = event.eventTimeMarks
}
