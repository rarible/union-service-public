package com.rarible.protocol.union.subscriber

import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.UnionEventTopicProvider
import java.util.*

class UnionEventsConsumerFactory(
    private val brokerReplicaSet: String,
    host: String,
    private val environment: String
) {

    private val clientIdPrefix = "$environment.$host.${UUID.randomUUID()}"

    fun createItemConsumer(consumerGroup: String): RaribleKafkaConsumer<ItemEventDto> {
        return RaribleKafkaConsumer(
            clientId = "$clientIdPrefix.union-item-consumer",
            valueDeserializerClass = UnionKafkaJsonDeserializer::class.java,
            valueClass = ItemEventDto::class.java,
            consumerGroup = consumerGroup,
            defaultTopic = UnionEventTopicProvider.getItemTopic(environment),
            bootstrapServers = brokerReplicaSet
        )
    }

    fun createOwnershipConsumer(consumerGroup: String): RaribleKafkaConsumer<OwnershipEventDto> {
        return RaribleKafkaConsumer(
            clientId = "$clientIdPrefix.union-ownership-consumer",
            valueDeserializerClass = UnionKafkaJsonDeserializer::class.java,
            valueClass = OwnershipEventDto::class.java,
            consumerGroup = consumerGroup,
            defaultTopic = UnionEventTopicProvider.getOwnershipTopic(environment),
            bootstrapServers = brokerReplicaSet
        )
    }

    fun createOrderConsumer(consumerGroup: String): RaribleKafkaConsumer<OrderEventDto> {
        return RaribleKafkaConsumer(
            clientId = "$clientIdPrefix.union-order-consumer",
            valueDeserializerClass = UnionKafkaJsonDeserializer::class.java,
            valueClass = OrderEventDto::class.java,
            consumerGroup = consumerGroup,
            defaultTopic = UnionEventTopicProvider.getOrderTopic(environment),
            bootstrapServers = brokerReplicaSet
        )
    }

    // TODO add activity/order consumers
}