package com.rarible.protocol.union.subscriber.autoconfigure

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.union.subscriber.UnionEventsConsumerFactory
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@SpringBootTest(
    properties = [
        "protocol.union.subscriber.broker-replicaset=PLAINTEXT://localhost:9092"
    ]
)
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(UnionEventsSubscriberAutoConfigurationIt.Configuration::class)
class UnionEventsSubscriberAutoConfigurationIt {

    @Autowired
    private lateinit var unionEventsConsumerFactory: UnionEventsConsumerFactory

    @Test
    fun `test default consumer initialized`() {
        Assertions.assertThat(unionEventsConsumerFactory).isNotNull
    }

    @TestConfiguration
    internal class Configuration {
        @Bean
        fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
            return ApplicationEnvironmentInfo("test", "test.com")
        }
    }
}
