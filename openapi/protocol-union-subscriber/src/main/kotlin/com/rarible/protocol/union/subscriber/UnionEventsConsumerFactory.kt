package com.rarible.protocol.union.subscriber

import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.protocol.dto.UnionEventTopicProvider
import com.rarible.protocol.union.dto.UnionItemEventDto
import com.rarible.protocol.union.dto.UnionOrderEventDto
import com.rarible.protocol.union.dto.UnionOwnershipEventDto
import java.util.*

class UnionEventsConsumerFactory(
    private val brokerReplicaSet: String,
    host: String,
    private val environment: String
) {

    private val clientIdPrefix = "$environment.$host.${UUID.randomUUID()}"

    fun createUnionItemConsumer(consumerGroup: String): RaribleKafkaConsumer<UnionItemEventDto> {
        return RaribleKafkaConsumer(
            clientId = "$clientIdPrefix.union-item-consumer",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = UnionItemEventDto::class.java,
            consumerGroup = consumerGroup,
            defaultTopic = UnionEventTopicProvider.getItemTopic(environment),
            bootstrapServers = brokerReplicaSet
        )
    }

    fun createUnionOwnershipConsumer(consumerGroup: String): RaribleKafkaConsumer<UnionOwnershipEventDto> {
        return RaribleKafkaConsumer(
            clientId = "$clientIdPrefix.union-ownership-consumer",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = UnionOwnershipEventDto::class.java,
            consumerGroup = consumerGroup,
            defaultTopic = UnionEventTopicProvider.getOwnershipTopic(environment),
            bootstrapServers = brokerReplicaSet
        )
    }

    fun createUnionOrderConsumer(consumerGroup: String): RaribleKafkaConsumer<UnionOrderEventDto> {
        return RaribleKafkaConsumer(
            clientId = "$clientIdPrefix.union-order-consumer",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = UnionOrderEventDto::class.java,
            consumerGroup = consumerGroup,
            defaultTopic = UnionEventTopicProvider.getOrderTopic(environment),
            bootstrapServers = brokerReplicaSet
        )
    }
}