package com.rarible.protocol.union.core

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.event.EventCountMetrics
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.event.OutgoingActivityEventListener
import com.rarible.protocol.union.core.event.OutgoingCollectionEventListener
import com.rarible.protocol.union.core.event.OutgoingEventListener
import com.rarible.protocol.union.core.event.CountingOutgoingEventListener
import com.rarible.protocol.union.core.event.OutgoingItemEventListener
import com.rarible.protocol.union.core.event.OutgoingOrderEventListener
import com.rarible.protocol.union.core.event.OutgoingOwnershipEventListener
import com.rarible.protocol.union.core.model.ActivityEvent
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OutgoingEventListenerConfiguration(
    val eventCountMetrics: EventCountMetrics,
) {
    @Bean
    fun outgoingActivityEventListener(eventsProducer: RaribleKafkaProducer<ActivityDto>): OutgoingEventListener<ActivityEvent> =
        CountingOutgoingEventListener(
            eventCountMetrics,
            OutgoingActivityEventListener(eventsProducer),
            EventType.ACTIVITY
        ) { event -> event.activity.id.blockchain }

    @Bean
    fun outgoingCollectionEventListener(eventsProducer: RaribleKafkaProducer<CollectionEventDto>): OutgoingEventListener<CollectionEventDto> =
        CountingOutgoingEventListener(
            eventCountMetrics,
            OutgoingCollectionEventListener(eventsProducer),
            EventType.COLLECTION
        ) { event -> event.collectionId.blockchain }

    @Bean
    fun outgoingItemEventListener(eventsProducer: RaribleKafkaProducer<ItemEventDto>): OutgoingEventListener<ItemEventDto> =
        CountingOutgoingEventListener(
            eventCountMetrics,
            OutgoingItemEventListener(eventsProducer),
            EventType.ITEM
        ) { event -> event.itemId.blockchain }

    @Bean
    fun outgoingOrderEventListener(eventsProducer: RaribleKafkaProducer<OrderEventDto>): OutgoingEventListener<OrderEventDto> =
        CountingOutgoingEventListener(
            eventCountMetrics,
            OutgoingOrderEventListener(eventsProducer),
            EventType.ORDER
        ) { event -> event.orderId.blockchain }

    @Bean
    fun outgoingOwnershipEventListener(eventsProducer: RaribleKafkaProducer<OwnershipEventDto>): OutgoingEventListener<OwnershipEventDto> =
        CountingOutgoingEventListener(
            eventCountMetrics,
            OutgoingOwnershipEventListener(eventsProducer),
            EventType.OWNERSHIP
        ) { event -> event.ownershipId.blockchain }
}
