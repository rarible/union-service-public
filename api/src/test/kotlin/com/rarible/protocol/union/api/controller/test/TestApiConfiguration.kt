package com.rarible.protocol.union.api.controller.test

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.Compression
import com.rarible.core.kafka.RaribleKafkaConsumerFactory
import com.rarible.core.kafka.RaribleKafkaConsumerSettings
import com.rarible.core.kafka.RaribleKafkaListenerContainerFactory
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.ext.KafkaTestExtension
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemEventTopicProvider
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipEventTopicProvider
import com.rarible.protocol.flow.nft.api.client.FlowNftCollectionControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftCryptoControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftItemControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftOrderActivityControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftOwnershipControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowOrderControllerApi
import com.rarible.protocol.nft.api.client.NftActivityControllerApi
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.order.api.client.AuctionActivityControllerApi
import com.rarible.protocol.order.api.client.OrderActivityControllerApi
import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.union.api.client.FixedUnionApiServiceUriProvider
import com.rarible.protocol.union.api.client.UnionApiClientFactory
import com.rarible.protocol.union.api.configuration.WebSocketConfiguration
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.elasticsearch.EsRepository
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.model.elastic.EsEntitiesConfig
import com.rarible.protocol.union.core.test.TestUnionEventHandler
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.FakeSubscriptionEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OrderUpdateEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.SubscriptionEventDto
import com.rarible.protocol.union.dto.SubscriptionRequestDto
import com.rarible.protocol.union.dto.UnionEventTopicProvider
import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import com.rarible.protocol.union.subscriber.UnionKafkaJsonSerializer
import com.rarible.protocol.union.test.mock.CurrencyMock
import io.mockk.coEvery
import io.mockk.mockk
import org.elasticsearch.action.support.IndicesOptions
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.web.context.WebServerInitializedEvent
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.reactive.DefaultReactiveElasticsearchClient
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate
import org.springframework.data.elasticsearch.core.RefreshPolicy
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Sinks
import java.net.URI
import java.util.concurrent.LinkedBlockingQueue
import com.rarible.protocol.solana.api.client.ActivityControllerApi as SolanaActivityControllerApi
import com.rarible.protocol.solana.api.client.CollectionControllerApi as SolanaCollectionControllerApi

@Lazy
@Configuration
@EnableAutoConfiguration
@Import(WebSocketConfiguration::class)
class TestApiConfiguration : ApplicationListener<WebServerInitializedEvent> {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val host = "test.com"
    private val env = "test"
    private val applicationEnvironmentInfo = ApplicationEnvironmentInfo(env, host)

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Bean
    fun shutdownMono(): Sinks.One<Void> = Sinks.one()

    @Bean
    fun webSocketEventsQueue() = LinkedBlockingQueue<SubscriptionEventDto>()

    @Bean
    fun webSocketRequests(): Sinks.Many<List<SubscriptionRequestDto>> = Sinks.many().unicast().onBackpressureBuffer()

    @Bean
    @Qualifier("testLocalhostUri")
    fun testLocalhostUri(@LocalServerPort port: Int): URI {
        return URI("http://localhost:$port")
    }

    @Bean
    fun applicationEnvironmentInfo() = applicationEnvironmentInfo

    @Bean
    fun testRestTemplate(mapper: ObjectMapper): RestTemplate {
        val converter = MappingJackson2HttpMessageConverter()
        converter.setObjectMapper(mapper)
        val template = RestTemplate()
        template.messageConverters.add(0, converter)
        return template
    }

    @Bean
    @Primary
    fun testItemEventProducer(): RaribleKafkaProducer<ItemEventDto> =
        mockk { coEvery { close() } returns Unit }

    @Bean
    @Primary
    fun testCollectionEventProducer(): RaribleKafkaProducer<CollectionEventDto> =
        mockk { coEvery { close() } returns Unit }

    @Bean
    @Primary
    fun testOwnershipEventProducer(): RaribleKafkaProducer<OwnershipEventDto> =
        mockk { coEvery { close() } returns Unit }

    @Bean
    @Primary
    @Qualifier("download.scheduler.task.producer.item-meta")
    fun testDownloadTaskProducer(): RaribleKafkaProducer<DownloadTaskEvent> =
        mockk { coEvery { close() } returns Unit }

    @Bean
    @Primary
    fun testUnionApiClientFactory(@Qualifier("testLocalhostUri") uri: URI): UnionApiClientFactory {
        return UnionApiClientFactory(FixedUnionApiServiceUriProvider(uri))
    }

    // --------------------- UNION CLIENTS ---------------------//

    @Bean
    fun testItemControllerApi(factory: UnionApiClientFactory) = factory.createItemApiClient()

    @Bean
    fun testOwnershipControllerApi(factory: UnionApiClientFactory) = factory.createOwnershipApiClient()

    @Bean
    fun testOrderControllerApi(factory: UnionApiClientFactory) = factory.createOrderApiClient()

    @Bean
    fun testAuctionControllerApi(factory: UnionApiClientFactory) = factory.createAuctionApiClient()

    @Bean
    fun testSignatureControllerApi(factory: UnionApiClientFactory) = factory.createSignatureApiClient()

    @Bean
    fun testDomainControllerApi(factory: UnionApiClientFactory) = factory.createDomainApiClient()

    @Bean
    fun testCollectionControllerApi(factory: UnionApiClientFactory) = factory.createCollectionApiClient()

    @Bean
    fun testActivityControllerApi(factory: UnionApiClientFactory) = factory.createActivityApiClient()

    @Bean
    fun testCurrencyControllerApi(factory: UnionApiClientFactory) = factory.createCurrencyApiClient()

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
    fun testEthereumOrderApi(): OrderControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("ethereum.auction.api")
    fun testEthereumAuctionApi(): com.rarible.protocol.order.api.client.AuctionControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("ethereum.signature.api")
    fun testEthereumSignatureApi(): com.rarible.protocol.order.api.client.OrderSignatureControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("ethereum.domain.api")
    fun testEthereumDomainApi(): com.rarible.protocol.nft.api.client.NftDomainControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("ethereum.activity.api.item")
    fun testEthereumActivityItemApi(): NftActivityControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("ethereum.activity.api.order")
    fun testEthereumActivityOrderApi(): OrderActivityControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("ethereum.activity.api.auction")
    fun testEthereumActivityAuctionApi(): AuctionActivityControllerApi = mockk()

    // --------------------- POLYGON ---------------------//
    @Bean
    @Primary
    @Qualifier("polygon.item.api")
    fun testPolygonNftItemApi(): NftItemControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("polygon.ownership.api")
    fun testPolygonNftOwnershipApi(): NftOwnershipControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("polygon.collection.api")
    fun testPolygonNftCollectionApi(): NftCollectionControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("polygon.order.api")
    fun testPolygonOrderApi(): com.rarible.protocol.order.api.client.OrderControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("polygon.auction.api")
    fun testPolygonAuctionApi(): com.rarible.protocol.order.api.client.AuctionControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("polygon.signature.api")
    fun testPolygonSignatureApi(): com.rarible.protocol.order.api.client.OrderSignatureControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("polygon.activity.api.item")
    fun testPolygonActivityItemApi(): NftActivityControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("polygon.activity.api.order")
    fun testPolygonActivityOrderApi(): OrderActivityControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("polygon.activity.api.auction")
    fun testPolygonActivityAuctionApi(): AuctionActivityControllerApi = mockk()

    // --------------------- SOLANA -------------------//

    @Bean
    @Primary
    fun testSolanaActivityApi(): SolanaActivityControllerApi = mockk()

    @Bean
    @Primary
    fun testSolanaCollectionApi(): SolanaCollectionControllerApi = mockk()

    // --------------------- FLOW ---------------------//
    @Bean
    @Primary
    fun testFlowItemApi(): FlowNftItemControllerApi = mockk()

    @Bean
    @Primary
    fun testFlowOwnershipApi(): FlowNftOwnershipControllerApi = mockk()

    @Bean
    @Primary
    fun testFlowCollectionApi(): FlowNftCollectionControllerApi = mockk()

    @Bean
    @Primary
    fun testFlowOrderApi(): FlowOrderControllerApi = mockk()

    @Bean
    @Primary
    fun testFlowSignatureApi(): FlowNftCryptoControllerApi = mockk()

    @Bean
    @Primary
    fun testFlowActivityApi(): FlowNftOrderActivityControllerApi = mockk()

    // --------------------- TEZOS ---------------------//

    @Bean
    @Primary
    fun testDipDupOrderClient(): com.rarible.dipdup.client.OrderClient = mockk()

    @Bean
    @Primary
    fun testTokenActivityClient(): com.rarible.tzkt.client.TokenActivityClient = mockk()

    @Bean
    @Primary
    fun testTzktTokenClient(): com.rarible.tzkt.client.TokenClient = mockk()

    @Bean
    @Primary
    fun testTzktOwnershipClient(): com.rarible.tzkt.client.OwnershipClient = mockk()

    @Bean
    @Primary
    fun testTzktCollectionClient(): com.rarible.tzkt.client.CollectionClient = mockk()

    @Bean
    @Primary
    fun testSignatureClient(): com.rarible.tzkt.client.SignatureClient = mockk()

    // ---------------------- KAFKA ---------------------//
    @Bean
    fun testItemHandler() = TestUnionEventHandler<ItemEventDto>()

    @Bean
    fun testItemConsumer(
        handler: TestUnionEventHandler<ItemEventDto>,
        kafkaConsumerFactory: RaribleKafkaConsumerFactory,
        itemContainerFactory: RaribleKafkaListenerContainerFactory<ItemEventDto>,
    ): ConcurrentMessageListenerContainer<String, ItemEventDto> {
        val topic = UnionEventTopicProvider.getItemTopic(applicationEnvironmentInfo.name)
        val settings = RaribleKafkaConsumerSettings(
            topic = topic,
            group = "test-union-item-group",
            async = false,
        )
        return kafkaConsumerFactory.createWorker(settings, handler, itemContainerFactory)
    }

    @Bean
    fun testOwnershipHandler() = TestUnionEventHandler<OwnershipEventDto>()

    @Bean
    fun testOwnershipConsumer(
        handler: TestUnionEventHandler<OwnershipEventDto>,
        kafkaConsumerFactory: RaribleKafkaConsumerFactory,
        ownershipContainerFactory: RaribleKafkaListenerContainerFactory<OwnershipEventDto>,
    ): ConcurrentMessageListenerContainer<String, OwnershipEventDto> {
        val topic = UnionEventTopicProvider.getOwnershipTopic(applicationEnvironmentInfo.name)
        val settings = RaribleKafkaConsumerSettings(
            topic = topic,
            group = "test-union-ownership-group",
            async = false,
        )
        return kafkaConsumerFactory.createWorker(settings, handler, ownershipContainerFactory)
    }

    @Bean
    fun testEthereumItemEventProducer(): RaribleKafkaProducer<NftItemEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.ethereum.item",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = NftItemEventDto::class.java,
            defaultTopic = NftItemEventTopicProvider.getTopic(applicationEnvironmentInfo.name, "ethereum"),
            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers(),
            compression = Compression.SNAPPY,
        )
    }

    @Bean
    fun testEthereumOwnershipEventProducer(): RaribleKafkaProducer<NftOwnershipEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.ethereum.ownership",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = NftOwnershipEventDto::class.java,
            defaultTopic = NftOwnershipEventTopicProvider.getTopic(applicationEnvironmentInfo.name, "ethereum"),
            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers(),
            compression = Compression.SNAPPY,
        )
    }

    @Bean
    @Qualifier("item.producer.api")
    fun unionItemEventProducer(): RaribleKafkaProducer<ItemEventDto> {
        val itemTopic = UnionEventTopicProvider.getItemTopic("test")
        return createUnionProducer("item", itemTopic, ItemEventDto::class.java)
    }

    @Bean
    @Qualifier("order.producer.api")
    fun unionOrderEventProducer(): RaribleKafkaProducer<OrderUpdateEventDto> {
        val orderTopic = UnionEventTopicProvider.getOrderTopic("test")
        return createUnionProducer("order", orderTopic, OrderUpdateEventDto::class.java)
    }

    private fun <T> createUnionProducer(clientSuffix: String, topic: String, type: Class<T>): RaribleKafkaProducer<T> {
        return RaribleKafkaProducer(
            clientId = "test.protocol-union-service.$clientSuffix",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = type,
            defaultTopic = topic,
            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers(),
            compression = Compression.SNAPPY,
        )
    }

    // ---------------------- ES -----------------------//

    @Bean
    @Primary
    fun testDipDupTokenClient(): com.rarible.dipdup.client.TokenClient = mockk()

    @Bean(initMethod = "bootstrap")
    @ConditionalOnMissingBean
    fun elasticsearchBootstrap(
        @Qualifier("esOperationsWithTimeout") reactiveElasticSearchOperations: ReactiveElasticsearchOperations,
        restHighLevelClient: ReactiveElasticsearchClient,
        esNameResolver: EsNameResolver,
        indexService: IndexService,
        repositories: List<EsRepository>,
    ): ElasticsearchTestBootstrapper {

        return ElasticsearchTestBootstrapper(
            esNameResolver = esNameResolver,
            esOperations = reactiveElasticSearchOperations,
            restHighLevelClient,
            entityDefinitions = EsEntitiesConfig.createEsEntities(),
            repositories = repositories
        )
    }

    @Primary
    @Bean("esOperationsWithTimeout")
    fun elasticSearchOperations(
        clientConfiguration: ClientConfiguration,
        converter: ElasticsearchConverter,
    ): ReactiveElasticsearchOperations {
        val configWithTimeout = ClientConfiguration.builder()
            .connectedTo(*clientConfiguration.endpoints.toTypedArray())
            .withConnectTimeout(15000)
            .withSocketTimeout(15000)
            .build()
        val client = DefaultReactiveElasticsearchClient.create(configWithTimeout)

        val template = ReactiveElasticsearchTemplate(client, converter)
        template.setIndicesOptions(IndicesOptions.strictExpandOpenAndForbidClosed())
        template.refreshPolicy = RefreshPolicy.IMMEDIATE
        return template
    }

    override fun onApplicationEvent(event: WebServerInitializedEvent) {
        val typeRef = object : TypeReference<List<SubscriptionRequestDto>>() {}
        val port = event.webServer.port

        ReactorNettyWebSocketClient().execute(URI("ws://127.0.0.1:$port/v0.1/subscribe")) { session ->
            logger.info("Connected to ws")
            session.receive().map { notification ->
                logger.info("received from ws: {}", notification.payloadAsText)
                val evt: SubscriptionEventDto = objectMapper.readValue(notification.payloadAsText)
                if (evt !is FakeSubscriptionEventDto) {
                    webSocketEventsQueue().offer(evt)
                }
            }.subscribe()

            session.send(webSocketRequests().asFlux().map {
                val bytes = objectMapper.writerFor(typeRef).writeValueAsBytes(it)
                logger.info("sending to ws: {}", String(bytes))
                WebSocketMessage(
                    WebSocketMessage.Type.TEXT, DefaultDataBufferFactory().wrap(bytes)
                )
            }).doOnSubscribe { logger.info("subscribed to ws") }
                .subscribe({}, { logger.error("ws error", it) }, { logger.info("disconnected from ws") })
            shutdownMono().asMono()
        }.subscribe()
    }
}
