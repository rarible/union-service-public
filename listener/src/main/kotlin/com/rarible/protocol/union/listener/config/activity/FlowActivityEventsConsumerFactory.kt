package com.rarible.protocol.union.listener.config.activity

import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.protocol.dto.FlowActivityDto
import java.util.*

class FlowActivityEventsConsumerFactory(
    private val brokerReplicaSet: String,
    private val host: String,
    private val environment: String
) {

    fun createActivityConsumer(consumerGroup: String): RaribleKafkaConsumer<FlowActivityDto> {
        return RaribleKafkaConsumer(
            clientId = "${createClientIdPrefix()}.flow-nft-order-activity-consumer",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = FlowActivityDto::class.java,
            consumerGroup = consumerGroup,
            defaultTopic = FlowActivityTopicProvider.getTopic(environment),
            bootstrapServers = brokerReplicaSet
        )
    }

    private fun createClientIdPrefix(): String {
        return "$environment.flow.$host.${UUID.randomUUID()}"
    }
}
