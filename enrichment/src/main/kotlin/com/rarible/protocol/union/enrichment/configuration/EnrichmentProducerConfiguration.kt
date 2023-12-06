package com.rarible.protocol.union.enrichment.configuration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.event.CountingOutgoingEventListener
import com.rarible.protocol.union.core.event.EventCountMetrics
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.event.OutgoingActivityEventListener
import com.rarible.protocol.union.core.event.OutgoingCollectionEventListener
import com.rarible.protocol.union.core.event.OutgoingEventListener
import com.rarible.protocol.union.core.event.OutgoingItemEventListener
import com.rarible.protocol.union.core.event.OutgoingOrderEventListener
import com.rarible.protocol.union.core.event.OutgoingOwnershipEventListener
import com.rarible.protocol.union.core.event.UnionInternalTopicProvider
import com.rarible.protocol.union.core.model.ActivityEvent
import com.rarible.protocol.union.core.model.ReconciliationMarkEvent
import com.rarible.protocol.union.core.model.UnionInternalBlockchainEvent
import com.rarible.protocol.union.core.producer.UnionInternalBlockchainEventProducer
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.UnionEventTopicProvider
import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import com.rarible.protocol.union.enrichment.model.ItemChangeEvent
import com.rarible.protocol.union.subscriber.UnionKafkaJsonSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EnrichmentProducerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val properties: ProducerProperties,
    private val eventCountMetrics: EventCountMetrics
) {

    private val env = applicationEnvironmentInfo.name

    @Bean
    fun reconciliationMarkEventProducer(): RaribleKafkaProducer<ReconciliationMarkEvent> {
        val topic = UnionInternalTopicProvider.getReconciliationMarkTopic(env)
        return createUnionProducer("reconciliation", topic, ReconciliationMarkEvent::class.java)
    }

    @Bean
    @Qualifier("download.scheduler.task.producer.item-meta")
    fun itemDownloadTaskProducer(): RaribleKafkaProducer<DownloadTaskEvent> {
        val topic = UnionInternalTopicProvider.getItemMetaDownloadTaskSchedulerTopic(env)
        return createUnionProducer("meta.publisher", topic, DownloadTaskEvent::class.java)
    }

    @Bean
    @Qualifier("download.scheduler.task.producer.collection-meta")
    fun collectionDownloadTaskProducer(): RaribleKafkaProducer<DownloadTaskEvent> {
        val topic = UnionInternalTopicProvider.getCollectionMetaDownloadTaskSchedulerTopic(env)
        return createUnionProducer("meta.publisher", topic, DownloadTaskEvent::class.java)
    }

    @Bean
    fun collectionEventProducer(): RaribleKafkaProducer<CollectionEventDto> {
        val collectionTopic = UnionEventTopicProvider.getCollectionTopic(env)
        return createUnionProducer("collection", collectionTopic, CollectionEventDto::class.java)
    }

    @Bean
    fun itemEventProducer(): RaribleKafkaProducer<ItemEventDto> {
        val itemTopic = UnionEventTopicProvider.getItemTopic(env)
        return createUnionProducer("item", itemTopic, ItemEventDto::class.java)
    }

    @Bean
    fun ownershipEventProducer(): RaribleKafkaProducer<OwnershipEventDto> {
        val ownershipTopic = UnionEventTopicProvider.getOwnershipTopic(env)
        return createUnionProducer("ownership", ownershipTopic, OwnershipEventDto::class.java)
    }

    @Bean
    fun orderEventProducer(): RaribleKafkaProducer<OrderEventDto> {
        val orderTopic = UnionEventTopicProvider.getOrderTopic(env)
        return createUnionProducer("order", orderTopic, OrderEventDto::class.java)
    }

    @Bean
    fun activityEventProducer(): RaribleKafkaProducer<ActivityDto> {
        val activityTopic = UnionEventTopicProvider.getActivityTopic(env)
        return createUnionProducer("activity", activityTopic, ActivityDto::class.java)
    }

    @Bean
    fun internalBlockchainEventProducer(): UnionInternalBlockchainEventProducer {
        val producers = HashMap<BlockchainDto, RaribleKafkaProducer<UnionInternalBlockchainEvent>>()
        // We can create producers for all blockchains, even for disabled (just to avoid NPE checks)
        BlockchainDto.values().forEach {
            val producer = createUnionProducer(
                clientSuffix = "blockchain.${it.name.lowercase()}",
                topic = UnionInternalTopicProvider.getInternalBlockchainTopic(env, it),
                type = UnionInternalBlockchainEvent::class.java
            )
            producers[it] = producer
        }
        return UnionInternalBlockchainEventProducer(producers)
    }

    @Bean
    fun itemChangeEventProducer(): RaribleKafkaProducer<ItemChangeEvent> {
        val topic = UnionInternalTopicProvider.getItemChangeTopic(env)
        return createUnionProducer("change.item.publisher", topic, ItemChangeEvent::class.java)
    }

    @Bean
    fun outgoingActivityEventListener(
        eventsProducer: RaribleKafkaProducer<ActivityDto>
    ): OutgoingEventListener<ActivityEvent> = CountingOutgoingEventListener(
        eventCountMetrics,
        OutgoingActivityEventListener(eventsProducer),
        EventType.ACTIVITY
    ) { event -> event.activity.id.blockchain }

    @Bean
    fun outgoingCollectionEventListener(
        eventsProducer: RaribleKafkaProducer<CollectionEventDto>
    ): OutgoingEventListener<CollectionEventDto> = CountingOutgoingEventListener(
        eventCountMetrics,
        OutgoingCollectionEventListener(eventsProducer),
        EventType.COLLECTION
    ) { event -> event.collectionId.blockchain }

    @Bean
    fun outgoingItemEventListener(
        eventsProducer: RaribleKafkaProducer<ItemEventDto>
    ): OutgoingEventListener<ItemEventDto> = CountingOutgoingEventListener(
        eventCountMetrics,
        OutgoingItemEventListener(eventsProducer),
        EventType.ITEM
    ) { event -> event.itemId.blockchain }

    @Bean
    fun outgoingOrderEventListener(
        eventsProducer: RaribleKafkaProducer<OrderEventDto>
    ): OutgoingEventListener<OrderEventDto> = CountingOutgoingEventListener(
        eventCountMetrics,
        OutgoingOrderEventListener(eventsProducer),
        EventType.ORDER
    ) { event -> event.orderId.blockchain }

    @Bean
    fun outgoingOwnershipEventListener(
        eventsProducer: RaribleKafkaProducer<OwnershipEventDto>
    ): OutgoingEventListener<OwnershipEventDto> = CountingOutgoingEventListener(
        eventCountMetrics,
        OutgoingOwnershipEventListener(eventsProducer),
        EventType.OWNERSHIP
    ) { event -> event.ownershipId.blockchain }

    private fun <T> createUnionProducer(clientSuffix: String, topic: String, type: Class<T>): RaribleKafkaProducer<T> {
        return RaribleKafkaProducer(
            clientId = "$env.protocol-union-service.$clientSuffix",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = type,
            defaultTopic = topic,
            bootstrapServers = properties.brokerReplicaSet,
            compression = properties.compression,
        )
    }
}
