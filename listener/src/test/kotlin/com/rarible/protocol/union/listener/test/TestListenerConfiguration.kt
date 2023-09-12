package com.rarible.protocol.union.listener.test

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.content.meta.loader.ContentMetaReceiver
import com.rarible.core.kafka.Compression
import com.rarible.core.kafka.RaribleKafkaConsumerFactory
import com.rarible.core.kafka.RaribleKafkaConsumerSettings
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.ext.KafkaTestExtension.Companion.kafkaContainer
import com.rarible.dipdup.client.core.model.DipDupActivity
import com.rarible.dipdup.client.core.model.DipDupOrder
import com.rarible.dipdup.listener.config.DipDupTopicProvider
import com.rarible.dipdup.listener.model.DipDupCollectionEvent
import com.rarible.dipdup.listener.model.DipDupItemMetaEvent
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.dto.ActivityTopicProvider
import com.rarible.protocol.dto.EthActivityEventDto
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowActivityEventTopicProvider
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowNftItemEventTopicProvider
import com.rarible.protocol.dto.FlowNftOwnershipEventTopicProvider
import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOrderEventTopicProvider
import com.rarible.protocol.dto.FlowOwnershipEventDto
import com.rarible.protocol.dto.NftCollectionEventTopicProvider
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemEventTopicProvider
import com.rarible.protocol.dto.NftItemMetaEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipEventTopicProvider
import com.rarible.protocol.dto.OrderIndexerTopicProvider
import com.rarible.protocol.flow.nft.api.client.FlowNftItemControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftOwnershipControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowOrderControllerApi
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.solana.api.client.TokenControllerApi
import com.rarible.protocol.solana.dto.SolanaEventTopicProvider
import com.rarible.protocol.solana.dto.TokenMetaEventDto
import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.core.event.UnionInternalTopicProvider
import com.rarible.protocol.union.core.test.TestUnionEventHandler
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.UnionEventTopicProvider
import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import com.rarible.protocol.union.subscriber.UnionKafkaJsonSerializer
import com.rarible.protocol.union.test.mock.CurrencyMock
import io.mockk.mockk
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary

@TestConfiguration
@Import(CoreConfiguration::class)
class TestListenerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val kafkaConsumerFactory: RaribleKafkaConsumerFactory
) {

    private val env = applicationEnvironmentInfo.name

    @Bean
    @Primary
    @Qualifier("test.content.meta.receiver")
    fun testContentMetaReceiver(): ContentMetaReceiver = mockk()

    // ----------------- UNION CONSUMERS ------------------//
    // Test consumers with EARLIEST offset

    @Bean
    fun testCollectionHandler() = TestUnionEventHandler<CollectionEventDto>()

    @Bean
    fun testCollectionConsumer(
        handler: TestUnionEventHandler<CollectionEventDto>
    ): RaribleKafkaConsumerWorker<CollectionEventDto> {
        val topic = UnionEventTopicProvider.getCollectionTopic(env)
        val settings = RaribleKafkaConsumerSettings(
            hosts = kafkaContainer.kafkaBoostrapServers(),
            topic = topic,
            group = "test-union-collection-group",
            concurrency = 1,
            batchSize = 10,
            async = false,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
            valueClass = CollectionEventDto::class.java
        )
        return kafkaConsumerFactory.createWorker(settings, handler)
    }

    @Bean
    fun testItemHandler() = TestUnionEventHandler<ItemEventDto>()

    @Bean
    fun testItemConsumer(
        handler: TestUnionEventHandler<ItemEventDto>
    ): RaribleKafkaConsumerWorker<ItemEventDto> {
        val topic = UnionEventTopicProvider.getItemTopic(env)
        val settings = RaribleKafkaConsumerSettings(
            hosts = kafkaContainer.kafkaBoostrapServers(),
            topic = topic,
            group = "test-union-item-group",
            concurrency = 1,
            batchSize = 10,
            async = false,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
            valueClass = ItemEventDto::class.java
        )
        return kafkaConsumerFactory.createWorker(settings, handler)
    }

    @Bean
    fun testOwnershipHandler() = TestUnionEventHandler<OwnershipEventDto>()

    @Bean
    fun testOwnershipConsumer(
        handler: TestUnionEventHandler<OwnershipEventDto>
    ): RaribleKafkaConsumerWorker<OwnershipEventDto> {
        val topic = UnionEventTopicProvider.getOwnershipTopic(env)
        val settings = RaribleKafkaConsumerSettings(
            hosts = kafkaContainer.kafkaBoostrapServers(),
            topic = topic,
            group = "test-union-ownership-group",
            concurrency = 1,
            batchSize = 10,
            async = false,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
            valueClass = OwnershipEventDto::class.java
        )
        return kafkaConsumerFactory.createWorker(settings, handler)
    }

    @Bean
    fun testOrderHandler() = TestUnionEventHandler<OrderEventDto>()

    @Bean
    fun testOrderConsumer(
        handler: TestUnionEventHandler<OrderEventDto>
    ): RaribleKafkaConsumerWorker<OrderEventDto> {
        val topic = UnionEventTopicProvider.getOrderTopic(env)
        val settings = RaribleKafkaConsumerSettings(
            hosts = kafkaContainer.kafkaBoostrapServers(),
            topic = topic,
            group = "test-union-order-group",
            concurrency = 1,
            batchSize = 10,
            async = false,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
            valueClass = OrderEventDto::class.java
        )
        return kafkaConsumerFactory.createWorker(settings, handler)
    }

    @Bean
    fun testActivityHandler() = TestUnionEventHandler<ActivityDto>()

    @Bean
    fun testActivityConsumer(handler: TestUnionEventHandler<ActivityDto>): RaribleKafkaConsumerWorker<ActivityDto> {
        val topic = UnionEventTopicProvider.getActivityTopic(env)
        val settings = RaribleKafkaConsumerSettings(
            hosts = kafkaContainer.kafkaBoostrapServers(),
            topic = topic,
            group = "test-union-activity-group",
            concurrency = 1,
            batchSize = 10,
            async = false,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
            valueClass = ActivityDto::class.java
        )
        return kafkaConsumerFactory.createWorker(settings, handler)
    }

    @Bean
    fun testDownloadTaskEventHandler() = TestUnionEventHandler<DownloadTaskEvent>()

    @Bean
    fun testDownloadTaskEventConsumer(handler: TestUnionEventHandler<DownloadTaskEvent>): RaribleKafkaConsumerWorker<DownloadTaskEvent> {
        val topic = UnionInternalTopicProvider.getItemMetaDownloadTaskSchedulerTopic(env)
        val settings = RaribleKafkaConsumerSettings(
            hosts = kafkaContainer.kafkaBoostrapServers(),
            topic = topic,
            group = "test-union-download-task-event-group",
            concurrency = 1,
            batchSize = 10,
            async = false,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
            valueClass = DownloadTaskEvent::class.java
        )
        return kafkaConsumerFactory.createWorker(settings, handler)
    }

    // ---------------- ETHEREUM producers ----------------//

    @Bean
    fun testEthereumItemEventProducer(): RaribleKafkaProducer<NftItemEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.ethereum.item",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = NftItemEventDto::class.java,
            defaultTopic = NftItemEventTopicProvider.getTopic(env, "ethereum"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            compression = Compression.SNAPPY,
        )
    }

    @Bean
    fun testEthereumItemMetaEventProducer(): RaribleKafkaProducer<NftItemMetaEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.ethereum.item.meta",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = NftItemMetaEventDto::class.java,
            defaultTopic = NftItemEventTopicProvider.getItemMetaTopic(env, "ethereum"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            compression = Compression.SNAPPY,
        )
    }

    @Bean
    fun testEthereumOwnershipEventProducer(): RaribleKafkaProducer<NftOwnershipEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.ethereum.ownership",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = NftOwnershipEventDto::class.java,
            defaultTopic = NftOwnershipEventTopicProvider.getTopic(env, "ethereum"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            compression = Compression.SNAPPY,
        )
    }

    @Bean
    fun testEthereumOrderEventProducer(): RaribleKafkaProducer<com.rarible.protocol.dto.OrderEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.ethereum.order",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = com.rarible.protocol.dto.OrderEventDto::class.java,
            defaultTopic = OrderIndexerTopicProvider.getOrderUpdateTopic(env, "ethereum"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            compression = Compression.SNAPPY,
        )
    }

    @Bean
    fun testDipDupOrderEventProducer(): RaribleKafkaProducer<DipDupOrder> {
        return RaribleKafkaProducer(
            clientId = "test.union.tezos.order",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = DipDupOrder::class.java,
            defaultTopic = DipDupTopicProvider.getOrderTopic("test"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            compression = Compression.SNAPPY,
        )
    }

    @Bean
    fun testDipDupActivityEventProducer(): RaribleKafkaProducer<DipDupActivity> {
        return RaribleKafkaProducer(
            clientId = "test.union.tezos.activity",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = DipDupActivity::class.java,
            defaultTopic = DipDupTopicProvider.getActivityTopic("test"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            compression = Compression.SNAPPY,
        )
    }

    @Bean
    fun testDipDupItemMetaEventProducer(): RaribleKafkaProducer<DipDupItemMetaEvent> {
        return RaribleKafkaProducer(
            clientId = "test.union.tezos.item.meta",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = DipDupItemMetaEvent::class.java,
            defaultTopic = DipDupTopicProvider.getItemMetaTopic("test"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            compression = Compression.SNAPPY,
        )
    }

    @Bean
    fun testDipDupCollectionEventProducer(): RaribleKafkaProducer<DipDupCollectionEvent> {
        return RaribleKafkaProducer(
            clientId = "test.union.tezos.collection",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = DipDupCollectionEvent::class.java,
            defaultTopic = DipDupTopicProvider.getCollectionTopic("test"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            compression = Compression.SNAPPY,
        )
    }

    @Bean
    fun testEthereumCollectionEventProducer(): RaribleKafkaProducer<com.rarible.protocol.dto.NftCollectionEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.ethereum.activity",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = com.rarible.protocol.dto.NftCollectionEventDto::class.java,
            defaultTopic = NftCollectionEventTopicProvider.getTopic(env, "ethereum"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            compression = Compression.SNAPPY,
        )
    }

    @Bean
    fun testEthereumActivityEventProducer(): RaribleKafkaProducer<EthActivityEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.ethereum.activity",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = EthActivityEventDto::class.java,
            defaultTopic = ActivityTopicProvider.getActivityTopic(env, "ethereum"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            compression = Compression.SNAPPY,
        )
    }

    // ------------------ FLOW producers ------------------//

    @Bean
    fun testFlowItemEventProducer(): RaribleKafkaProducer<FlowNftItemEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.flow.item",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = FlowNftItemEventDto::class.java,
            defaultTopic = FlowNftItemEventTopicProvider.getTopic(env),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            compression = Compression.SNAPPY,
        )
    }

    @Bean
    fun testFlowOwnershipEventProducer(): RaribleKafkaProducer<FlowOwnershipEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.flow.ownership",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = FlowOwnershipEventDto::class.java,
            defaultTopic = FlowNftOwnershipEventTopicProvider.getTopic(env),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            compression = Compression.SNAPPY,
        )
    }

    @Bean
    fun testFlowOrderEventProducer(): RaribleKafkaProducer<FlowOrderEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.flow.order",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = FlowOrderEventDto::class.java,
            defaultTopic = FlowOrderEventTopicProvider.getTopic(env),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            compression = Compression.SNAPPY,
        )
    }

    @Bean
    fun testFlowActivityEventProducer(): RaribleKafkaProducer<FlowActivityDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.flow.activity",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = FlowActivityDto::class.java,
            defaultTopic = FlowActivityEventTopicProvider.getActivityTopic(env),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            compression = Compression.SNAPPY,
        )
    }

    // ---------------- SOLANA producers ----------------//

    @Bean
    fun testSolanaTokenMetaEventProducer(): RaribleKafkaProducer<TokenMetaEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.solana.token.meta",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = TokenMetaEventDto::class.java,
            defaultTopic = SolanaEventTopicProvider.getTokenMetaTopic(env),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            compression = Compression.SNAPPY,
        )
    }

    @Bean
    @Primary
    @Qualifier("solana.token.api")
    fun testSolanaTokenApi(): TokenControllerApi = mockk()

    // --------------------- CURRENCY ---------------------//

    @Bean
    @Primary
    fun testCurrencyApi(): CurrencyControllerApi = CurrencyMock.currencyControllerApiMock

    // --------------------- ETHEREUM ---------------------//
    @Bean
    @Primary
    @Qualifier("ethereum.item.api")
    fun testEthereumItemApi(): NftItemControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("ethereum.ownership.api")
    fun testEthereumOwnershipApi(): NftOwnershipControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("ethereum.collection.api")
    fun testEthereumCollectionApi(): NftCollectionControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("ethereum.order.api")
    fun testEthereumOrderApi(): com.rarible.protocol.order.api.client.OrderControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("ethereum.auction.api")
    fun testEthereumAuctionApi(): com.rarible.protocol.order.api.client.AuctionControllerApi = mockk()

    // --------------------- FLOW ---------------------//
    @Bean
    @Primary
    fun testFlowItemApi(): FlowNftItemControllerApi = mockk()

    @Bean
    @Primary
    fun testFlowOwnershipApi(): FlowNftOwnershipControllerApi = mockk()

    @Bean
    @Primary
    fun testFlowOrderApi(): FlowOrderControllerApi = mockk()

    // --------------------- TEZOS ---------------------//

    @Bean
    @Primary
    fun testTezosTokenClient(): com.rarible.tzkt.client.TokenClient = mockk()

    @Bean
    @Primary
    fun testTezosOwnershipClient(): com.rarible.tzkt.client.OwnershipClient = mockk()

    @Bean
    @Primary
    fun testTezosCollectionClient(): com.rarible.tzkt.client.CollectionClient = mockk()
}
