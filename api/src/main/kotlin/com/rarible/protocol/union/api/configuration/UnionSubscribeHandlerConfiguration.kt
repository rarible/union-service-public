package com.rarible.protocol.union.api.configuration

import com.rarible.protocol.union.api.handler.UnionSubscribeItemEventHandler
import com.rarible.protocol.union.api.handler.UnionSubscribeOrderEventHandler
import com.rarible.protocol.union.api.handler.UnionSubscribeOwnershipEventHandler
import com.rarible.protocol.union.core.ConsumerFactory
import com.rarible.protocol.union.core.handler.BatchedConsumerWorker
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.subscriber.UnionEventsConsumerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
@EnableConfigurationProperties(value = [SubscribeProperties::class])
class UnionSubscribeHandlerConfiguration(
    private val unionEventsConsumerFactory: UnionEventsConsumerFactory,
    private val consumerFactory: ConsumerFactory,
    private val properties: SubscribeProperties
) {
    private val unionWorkerType = "subscribe"

    @Bean
    fun unionSubscribeItemWorker(
        unionItemEventHandler: UnionSubscribeItemEventHandler
    ): BatchedConsumerWorker<ItemEventDto> {
        val group = "${consumerFactory.unionSubscribeItemGroup}.${UUID.randomUUID()}"
        val consumer = unionEventsConsumerFactory.createItemConsumer(group)
        return consumerFactory.createUnionItemBatchedConsumerWorker(
            consumer = consumer,
            handler = unionItemEventHandler,
            daemonWorkerProperties = properties.daemon,
            workers = properties.workers,
            type = unionWorkerType
        )
    }

    @Bean
    fun unionSubscribeOwnershipWorker(
        unionOwnershipEventHandler: UnionSubscribeOwnershipEventHandler
    ): BatchedConsumerWorker<OwnershipEventDto> {
        val group = "${consumerFactory.unionSubscribeOwnershipGroup}.${UUID.randomUUID()}"
        val consumer = unionEventsConsumerFactory.createOwnershipConsumer(group)
        return consumerFactory.createUnionOwnershipBatchedConsumerWorker(
            consumer = consumer,
            handler = unionOwnershipEventHandler,
            daemonWorkerProperties = properties.daemon,
            workers = properties.workers,
            type = unionWorkerType
        )
    }

    @Bean
    fun unionSubscribeOrderWorker(
        handler: UnionSubscribeOrderEventHandler
    ): BatchedConsumerWorker<OrderEventDto> {
        val group = "${consumerFactory.unionSubscribeOrderGroup}.${UUID.randomUUID()}"
        val consumer = unionEventsConsumerFactory.createOrderConsumer(group)
        return consumerFactory.createUnionOrderBatchedConsumerWorker(
            consumer = consumer,
            handler = handler,
            daemonWorkerProperties = properties.daemon,
            workers = properties.workers,
            type = unionWorkerType
        )
    }

    @Bean
    fun unionSubscriberItemWorkerStartup(
        unionSubscribeItemWorker: BatchedConsumerWorker<ItemEventDto>
    ): CommandLineRunner = CommandLineRunner {
        unionSubscribeItemWorker.start()
    }

    @Bean
    fun unionSubscriberOrderWorkerStartup(
        unionSubscribeOrderWorker: BatchedConsumerWorker<OrderEventDto>
    ): CommandLineRunner = CommandLineRunner {
        unionSubscribeOrderWorker.start()
    }

    @Bean
    fun unionSubscriberOwnershipWorkerStartup(
        unionSubscribeOwnershipWorker: BatchedConsumerWorker<OwnershipEventDto>
    ): CommandLineRunner = CommandLineRunner {
        unionSubscribeOwnershipWorker.start()
    }
}