package com.rarible.protocol.union.listener.test

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.content.meta.loader.ContentMetaReceiver
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.ext.KafkaTestExtension.Companion.kafkaContainer
import com.rarible.dipdup.client.core.model.DipDupActivity
import com.rarible.dipdup.client.core.model.DipDupCollection
import com.rarible.dipdup.client.core.model.DipDupOrder
import com.rarible.dipdup.listener.config.DipDupTopicProvider
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.dto.ActivityTopicProvider
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
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.UnionEventTopicProvider
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaLoader

import com.rarible.protocol.union.subscriber.UnionKafkaJsonDeserializer
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
class TestListenerConfiguration {

    @Bean
    fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
        return ApplicationEnvironmentInfo("test", "test.com")
    }

    @Bean
    @Primary
    @Qualifier("test.union.meta.loader")
    fun testUnionMetaLoader(): ItemMetaLoader = mockk()

    @Bean
    @Primary
    @Qualifier("test.content.meta.receiver")
    fun testContentMetaReceiver(): ContentMetaReceiver = mockk()

    //----------------- UNION CONSUMERS ------------------//
    // Test consumers with EARLIEST offset

    @Bean
    fun testCollectionConsumer(): RaribleKafkaConsumer<CollectionEventDto> {
        val topic = UnionEventTopicProvider.getCollectionTopic(applicationEnvironmentInfo().name)
        return RaribleKafkaConsumer(
            clientId = "test-union-collection-consumer",
            consumerGroup = "test-union-collection-group",
            valueDeserializerClass = UnionKafkaJsonDeserializer::class.java,
            valueClass = CollectionEventDto::class.java,
            defaultTopic = topic,
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    @Bean
    fun testItemConsumer(): RaribleKafkaConsumer<ItemEventDto> {
        val topic = UnionEventTopicProvider.getItemTopic(applicationEnvironmentInfo().name)
        return RaribleKafkaConsumer(
            clientId = "test-union-item-consumer",
            consumerGroup = "test-union-item-group",
            valueDeserializerClass = UnionKafkaJsonDeserializer::class.java,
            valueClass = ItemEventDto::class.java,
            defaultTopic = topic,
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    @Bean
    fun testOwnershipConsumer(): RaribleKafkaConsumer<OwnershipEventDto> {
        val topic = UnionEventTopicProvider.getOwnershipTopic(applicationEnvironmentInfo().name)
        return RaribleKafkaConsumer(
            clientId = "test-union-ownership-consumer",
            consumerGroup = "test-union-ownership-group",
            valueDeserializerClass = UnionKafkaJsonDeserializer::class.java,
            valueClass = OwnershipEventDto::class.java,
            defaultTopic = topic,
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    @Bean
    fun testOrderConsumer(): RaribleKafkaConsumer<OrderEventDto> {
        val topic = UnionEventTopicProvider.getOrderTopic(applicationEnvironmentInfo().name)
        return RaribleKafkaConsumer(
            clientId = "test-union-order-consumer",
            consumerGroup = "test-union-order-group",
            valueDeserializerClass = UnionKafkaJsonDeserializer::class.java,
            valueClass = OrderEventDto::class.java,
            defaultTopic = topic,
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    @Bean
    fun testActivityConsumer(): RaribleKafkaConsumer<ActivityDto> {
        val topic = UnionEventTopicProvider.getActivityTopic(applicationEnvironmentInfo().name)
        return RaribleKafkaConsumer(
            clientId = "test-union-activity-consumer",
            consumerGroup = "test-union-activity-group",
            valueDeserializerClass = UnionKafkaJsonDeserializer::class.java,
            valueClass = ActivityDto::class.java,
            defaultTopic = topic,
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    //---------------- ETHEREUM producers ----------------//

    @Bean
    fun testEthereumItemEventProducer(): RaribleKafkaProducer<NftItemEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.ethereum.item",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = NftItemEventDto::class.java,
            defaultTopic = NftItemEventTopicProvider.getTopic(applicationEnvironmentInfo().name, "ethereum"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    fun testEthereumOwnershipEventProducer(): RaribleKafkaProducer<NftOwnershipEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.ethereum.ownership",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = NftOwnershipEventDto::class.java,
            defaultTopic = NftOwnershipEventTopicProvider.getTopic(applicationEnvironmentInfo().name, "ethereum"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    fun testEthereumOrderEventProducer(): RaribleKafkaProducer<com.rarible.protocol.dto.OrderEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.ethereum.order",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = com.rarible.protocol.dto.OrderEventDto::class.java,
            defaultTopic = OrderIndexerTopicProvider.getOrderUpdateTopic(applicationEnvironmentInfo().name, "ethereum"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    fun testDipDupOrderEventProducer(): RaribleKafkaProducer<DipDupOrder> {
        return RaribleKafkaProducer(
            clientId = "test.union.tezos.order",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = DipDupOrder::class.java,
            defaultTopic = DipDupTopicProvider.getOrderTopic("test"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    fun testDipDupActivityEventProducer(): RaribleKafkaProducer<DipDupActivity> {
        return RaribleKafkaProducer(
            clientId = "test.union.tezos.activity",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = DipDupActivity::class.java,
            defaultTopic = DipDupTopicProvider.getActivityTopic("test"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    fun testDipDupCollectionEventProducer(): RaribleKafkaProducer<DipDupCollection> {
        return RaribleKafkaProducer(
            clientId = "test.union.tezos.collection",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = DipDupCollection::class.java,
            defaultTopic = DipDupTopicProvider.getCollectionTopic("test"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    fun testEthereumCollectionEventProducer(): RaribleKafkaProducer<com.rarible.protocol.dto.NftCollectionEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.ethereum.activity",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = com.rarible.protocol.dto.NftCollectionEventDto::class.java,
            defaultTopic = NftCollectionEventTopicProvider.getTopic(applicationEnvironmentInfo().name, "ethereum"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    fun testEthereumActivityEventProducer(): RaribleKafkaProducer<com.rarible.protocol.dto.ActivityDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.ethereum.activity",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = com.rarible.protocol.dto.ActivityDto::class.java,
            defaultTopic = ActivityTopicProvider.getTopic(applicationEnvironmentInfo().name, "ethereum"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

    //------------------ FLOW producers ------------------//

    @Bean
    fun testFlowItemEventProducer(): RaribleKafkaProducer<FlowNftItemEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.flow.item",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = FlowNftItemEventDto::class.java,
            defaultTopic = FlowNftItemEventTopicProvider.getTopic(applicationEnvironmentInfo().name),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    fun testFlowOwnershipEventProducer(): RaribleKafkaProducer<FlowOwnershipEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.flow.ownership",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = FlowOwnershipEventDto::class.java,
            defaultTopic = FlowNftOwnershipEventTopicProvider.getTopic(applicationEnvironmentInfo().name),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    fun testFlowOrderEventProducer(): RaribleKafkaProducer<FlowOrderEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.flow.order",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = FlowOrderEventDto::class.java,
            defaultTopic = FlowOrderEventTopicProvider.getTopic(applicationEnvironmentInfo().name),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    fun testFlowActivityEventProducer(): RaribleKafkaProducer<FlowActivityDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.flow.activity",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = FlowActivityDto::class.java,
            defaultTopic = FlowActivityEventTopicProvider.getTopic(applicationEnvironmentInfo().name),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

    //---------------- SOLANA producers ----------------//

    @Bean
    fun testSolanaTokenMetaEventProducer(): RaribleKafkaProducer<TokenMetaEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.solana.token.meta",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = TokenMetaEventDto::class.java,
            defaultTopic = SolanaEventTopicProvider.getTokenMetaTopic(applicationEnvironmentInfo().name),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    @Primary
    @Qualifier("solana.token.api")
    fun testSolanaTokenApi(): TokenControllerApi = mockk()

    //--------------------- CURRENCY ---------------------//

    @Bean
    @Primary
    fun testCurrencyApi(): CurrencyControllerApi = CurrencyMock.currencyControllerApiMock

    //--------------------- ETHEREUM ---------------------//
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


    //--------------------- FLOW ---------------------//
    @Bean
    @Primary
    fun testFlowItemApi(): FlowNftItemControllerApi = mockk()

    @Bean
    @Primary
    fun testFlowOwnershipApi(): FlowNftOwnershipControllerApi = mockk()

    @Bean
    @Primary
    fun testFlowOrderApi(): FlowOrderControllerApi = mockk()

    //--------------------- TEZOS ---------------------//

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
