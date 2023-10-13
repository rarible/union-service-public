package com.rarible.protocol.union.core

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumerFactory
import com.rarible.protocol.currency.api.client.CurrencyApiClientFactory
import com.rarible.protocol.currency.api.client.CurrencyApiServiceUriProvider
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.union.api.ApiClient
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.AuctionService
import com.rarible.protocol.union.core.service.BalanceService
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.DomainService
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.core.service.dummy.DummyActivityService
import com.rarible.protocol.union.core.service.dummy.DummyAuctionService
import com.rarible.protocol.union.core.service.dummy.DummyBalanceService
import com.rarible.protocol.union.core.service.dummy.DummyCollectionService
import com.rarible.protocol.union.core.service.dummy.DummyDomainService
import com.rarible.protocol.union.core.service.dummy.DummyItemService
import com.rarible.protocol.union.core.service.dummy.DummyOrderService
import com.rarible.protocol.union.core.service.dummy.DummyOwnershipService
import com.rarible.protocol.union.core.service.dummy.DummySignatureService
import com.rarible.protocol.union.core.service.router.ActiveBlockchainProvider
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.service.router.BlockchainService
import com.rarible.protocol.union.core.service.router.EvmActivityService
import com.rarible.protocol.union.core.service.router.EvmAuctionService
import com.rarible.protocol.union.core.service.router.EvmBalanceService
import com.rarible.protocol.union.core.service.router.EvmBlockchainService
import com.rarible.protocol.union.core.service.router.EvmCollectionService
import com.rarible.protocol.union.core.service.router.EvmDomainService
import com.rarible.protocol.union.core.service.router.EvmItemService
import com.rarible.protocol.union.core.service.router.EvmOrderService
import com.rarible.protocol.union.core.service.router.EvmOwnershipService
import com.rarible.protocol.union.core.service.router.EvmSignatureService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.subscriber.UnionKafkaJsonDeserializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.http.MediaType
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@EnableScheduling
@ComponentScan(basePackageClasses = [CoreConfiguration::class])
@EnableConfigurationProperties(
    value = [
        FeatureFlagsProperties::class,
        EsActivityEnrichmentProperties::class,
        EsProperties::class,
        EsOptimizationProperties::class,
    ]
)
class CoreConfiguration(
    val applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    val activeBlockchainProvider: ActiveBlockchainProvider,
    val featureFlagsProperties: FeatureFlagsProperties,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val allBlockchains = BlockchainDto.values().toSet()

    @Autowired
    fun setMapKeyDotReplacement(mappingMongoConverter: MappingMongoConverter) {
        mappingMongoConverter.setMapKeyDotReplacement("__DOT__")
    }

    @Bean
    fun raribleKafkaConsumerFactory() = RaribleKafkaConsumerFactory(
        applicationEnvironmentInfo.name,
        applicationEnvironmentInfo.host,
        UnionKafkaJsonDeserializer::class.java
    )

    @Bean
    fun webClient(webClientCustomizer: UnionWebClientCustomizer): WebClient {
        val mapper = ApiClient.createDefaultObjectMapper()
        val strategies = ExchangeStrategies
            .builder()
            .codecs { configurer: ClientCodecConfigurer ->
                configurer.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(mapper, MediaType.APPLICATION_JSON))
                configurer.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(mapper, MediaType.APPLICATION_JSON))
            }.build()

        val webClient = WebClient.builder().exchangeStrategies(strategies)
        webClientCustomizer.customize(webClient)
        return webClient.build()
    }

    @Bean
    fun balanceServiceRouter(
        evmServices: List<EvmBalanceService>,
        services: List<BalanceService>,
    ) = createRouter(evmServices, services, BalanceService::class.java) { DummyBalanceService(it) }

    @Bean
    fun itemServiceRouter(
        evmServices: List<EvmItemService>,
        services: List<ItemService>,
    ) = createRouter(evmServices, services, ItemService::class.java) { DummyItemService(it) }

    @Bean
    fun ownershipServiceRouter(
        evmServices: List<EvmOwnershipService>,
        services: List<OwnershipService>,
    ) = createRouter(evmServices, services, OwnershipService::class.java) { DummyOwnershipService(it) }

    @Bean
    fun collectionServiceRouter(
        evmServices: List<EvmCollectionService>,
        services: List<CollectionService>,
    ) = createRouter(evmServices, services, CollectionService::class.java) { DummyCollectionService(it) }

    @Bean
    fun orderServiceRouter(
        evmServices: List<EvmOrderService>,
        services: List<OrderService>,
    ) = createRouter(evmServices, services, OrderService::class.java) { DummyOrderService(it) }

    @Bean
    fun auctionServiceRouter(
        evmServices: List<EvmAuctionService>,
        services: List<AuctionService>,
    ) = createRouter(evmServices, services, AuctionService::class.java) { DummyAuctionService(it) }

    @Bean
    fun activityServiceRouter(
        evmServices: List<EvmActivityService>,
        services: List<ActivityService>,
    ) = createRouter(evmServices, services, ActivityService::class.java) { DummyActivityService(it) }

    @Bean
    fun signatureServiceRouter(
        evmServices: List<EvmSignatureService>,
        services: List<SignatureService>,
    ) = createRouter(evmServices, services, SignatureService::class.java) { DummySignatureService(it) }

    @Bean
    fun domainServiceRouter(
        evmServices: List<EvmDomainService>,
        services: List<DomainService>,
    ) = createRouter(evmServices, services, DomainService::class.java) { DummyDomainService(it) }

    @Bean
    fun currencyApiFactory(
        currencyApiServiceUriProvider: CurrencyApiServiceUriProvider,
        webClientCustomizer: UnionWebClientCustomizer
    ): CurrencyApiClientFactory {
        return CurrencyApiClientFactory(currencyApiServiceUriProvider, webClientCustomizer)
    }

    @Bean
    fun currencyApi(factory: CurrencyApiClientFactory): CurrencyControllerApi {
        return factory.createCurrencyApiClient()
    }

    private fun <T : BlockchainService> createRouter(
        evmServices: List<EvmBlockchainService<T>>,
        services: List<T>,
        type: Class<T>,
        default: (blockchain: BlockchainDto) -> T
    ): BlockchainRouter<T> {
        val result = ArrayList(evmServices.flatMap { it.services } + services)
        val disabled = getDisabledBlockchains(result)
        disabled.forEach {
            result.add(default(it))
            logger.info(
                "${type.simpleName} for blockchain {} disabled or not implemented, replaced by dummy",
                it.name
            )
        }
        return BlockchainRouter(result, activeBlockchainProvider.blockchains.toList())
    }

    private fun <T : BlockchainService> getDisabledBlockchains(services: List<T>): List<BlockchainDto> {
        services.groupBy { it.blockchain }.forEach { e ->
            if (e.value.size > 1) {
                throw IllegalArgumentException(
                    "There are several implementations of service " +
                        "for blockchain ${e.key}: ${e.value.map { it.javaClass.name }}, should be only one" +
                        "implementation"
                )
            }
        }

        val disabledBlockchains = this.allBlockchains.toMutableSet()
        services.forEach { disabledBlockchains.remove(it.blockchain) }
        return disabledBlockchains.toList()
    }
}
