package com.rarible.protocol.union.subscriber.autoconfigure

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.union.subscriber.UnionEventsConsumerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@EnableConfigurationProperties(UnionEventsSubscriberProperties::class)
class UnionEventsSubscriberAutoConfiguration(
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val properties: UnionEventsSubscriberProperties
) {
    @Bean
    @ConditionalOnMissingBean(UnionEventsConsumerFactory::class)
    fun unionEventsConsumerFactory(): UnionEventsConsumerFactory {
        return UnionEventsConsumerFactory(
            brokerReplicaSet = properties.brokerReplicaSet,
            host = applicationEnvironmentInfo.host,
            environment = applicationEnvironmentInfo.name
        )
    }
}