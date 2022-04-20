package com.rarible.protocol.union.search.indexer.test

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.ext.KafkaTestExtension
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.UnionEventTopicProvider
import com.rarible.protocol.union.subscriber.UnionKafkaJsonSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy

@Lazy
@Configuration
class TestIndexerConfiguration {
    @Bean
    fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
        return ApplicationEnvironmentInfo("test", "test.com")
    }

    //---------------- UNION producers ----------------//

    @Bean
    fun testUnionActivityEventProducer(): RaribleKafkaProducer<ActivityDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.activity",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = ActivityDto::class.java,
            defaultTopic = UnionEventTopicProvider.getActivityTopic(applicationEnvironmentInfo().name),
            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    fun testUnionOrderEventProducer(): RaribleKafkaProducer<OrderEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.order",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = OrderEventDto::class.java,
            defaultTopic = UnionEventTopicProvider.getOrderTopic(applicationEnvironmentInfo().name),
            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    fun testUnionOwnershipEventProducer(): RaribleKafkaProducer<OwnershipEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.order",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = OwnershipEventDto::class.java,
            defaultTopic = UnionEventTopicProvider.getOwnershipTopic(applicationEnvironmentInfo().name),
            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers()
        )
    }
}
