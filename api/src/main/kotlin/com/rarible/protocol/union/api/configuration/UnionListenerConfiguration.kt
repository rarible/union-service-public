package com.rarible.protocol.union.api.configuration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.protocol.union.api.handler.UnionItemEventHandler
import com.rarible.protocol.union.api.handler.UnionOwnershipEventHandler
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.integration.ethereum.EthereumIntegrationProperties
import com.rarible.protocol.union.subscriber.UnionEventsConsumerFactory
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
class UnionListenerConfig(
    properties: EthereumIntegrationProperties,
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val meterRegistry: MeterRegistry
) {
    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host

    private val consumer = properties.consumer!!

    private val daemon = properties.daemon

    @Bean
    @Qualifier("nft.consumer.factory.websocket")
    fun ethereumNftIndexerConsumerWebsocketFactory(): UnionEventsConsumerFactory {
        val replicaSet = consumer.brokerReplicaSet
        return UnionEventsConsumerFactory(replicaSet!!, host, env)
    }

    @Bean
    fun ethereumItemWebsocketWorker(
        @Qualifier("nft.consumer.factory.websocket") factory: UnionEventsConsumerFactory,
        unionItemEventHandler: UnionItemEventHandler

    ): ConsumerWorker<ItemEventDto> {
        val consumer = factory.createItemConsumer(UUID.randomUUID().toString())
          return  ConsumerWorker(
          consumer = consumer,
          properties = daemon,
          eventHandler = unionItemEventHandler,
          meterRegistry = meterRegistry,
          workerName = "internal-websocket"
      )
    }

    @Bean
    fun ethereumOwnershipWebsocketWorker(
        @Qualifier("nft.consumer.factory.websocket") factory: UnionEventsConsumerFactory,
        unionOwnershipEventHandler: UnionOwnershipEventHandler

    ): ConsumerWorker<OwnershipEventDto> {
        val consumer = factory.createOwnershipConsumer(UUID.randomUUID().toString())
        return  ConsumerWorker(
            consumer = consumer,
            properties = daemon,
            eventHandler = unionOwnershipEventHandler,
            meterRegistry = meterRegistry,
            workerName = "internal-websocket"
        )
    }
}
