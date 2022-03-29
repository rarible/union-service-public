package com.rarible.protocol.union.search.indexer.test

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.ext.KafkaTestExtension
import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.UnionEventTopicProvider
import com.rarible.protocol.union.search.indexer.config.UnionSearchIndexerConfig
import com.rarible.protocol.union.subscriber.UnionKafkaJsonSerializer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy

@Configuration
@Lazy
//@ComponentScan(basePackages = ["com.rarible.protocol.union.search.core"])
class TestListenerConfiguration {

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
}
