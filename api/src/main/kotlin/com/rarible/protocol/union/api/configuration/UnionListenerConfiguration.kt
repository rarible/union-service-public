package com.rarible.protocol.union.api.configuration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.task.EnableRaribleTask
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.nft.api.subscriber.NftIndexerEventsConsumerFactory
import com.rarible.protocol.union.core.ConsumerFactory
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.handler.KafkaConsumerWorker
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.integration.ethereum.EthereumIntegrationProperties
import com.rarible.protocol.union.integration.ethereum.event.EthItemEventHandler
import com.rarible.protocol.union.integration.ethereum.event.EthereumItemEventHandler
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableRaribleTask
class UnionListenerConfig(
    private val consumerFactory: ConsumerFactory,
    properties: EthereumIntegrationProperties,
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
) {
    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host

    private val consumer = properties.consumer!!
    private val workers = properties.consumer!!.workers

    private val daemon = properties.daemon

    @Bean
    @Qualifier("ethereum.nft.consumer.factory.websocket")
    fun ethereumNftIndexerConsumerWebsocketFactory(): NftIndexerEventsConsumerFactory {
        val replicaSet = consumer.brokerReplicaSet
        return NftIndexerEventsConsumerFactory(replicaSet!!, host, env)
    }

    @Bean
    @Qualifier("ethereum.item.handler.websocket")
    fun ethereumItemEventWebsocketHandler(handler: IncomingEventHandler<UnionItemEvent>): EthItemEventHandler {
        return EthereumItemEventHandler(handler)
    }

    @Bean
    fun ethereumItemWebsocketWorker(
        @Qualifier("ethereum.nft.consumer.factory.websocket") factory: NftIndexerEventsConsumerFactory,
        @Qualifier("ethereum.item.handler.websocket") handler: EthItemEventHandler
    ): KafkaConsumerWorker<NftItemEventDto> {
        val consumer = factory.createItemEventsConsumer(consumerFactory.itemGroup, Blockchain.ETHEREUM)
        return consumerFactory.createItemConsumer(consumer, handler, daemon, workers)
    }
}
