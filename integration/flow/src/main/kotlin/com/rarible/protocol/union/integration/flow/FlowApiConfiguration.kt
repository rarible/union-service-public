package com.rarible.protocol.union.integration.flow

import com.rarible.protocol.flow.nft.api.client.FlowApiServiceUriProvider
import com.rarible.protocol.flow.nft.api.client.FlowBidOrderControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftCollectionControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftCryptoControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftIndexerApiClientFactory
import com.rarible.protocol.flow.nft.api.client.FlowNftItemControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftOrderActivityControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftOwnershipControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowOrderControllerApi
import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.core.client.customizer.UnionWebClientCustomizer
import com.rarible.protocol.union.core.service.AuctionService
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.OrderProxyService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.integration.flow.converter.FlowActivityConverter
import com.rarible.protocol.union.integration.flow.converter.FlowOrderConverter
import com.rarible.protocol.union.integration.flow.service.FlowActivityService
import com.rarible.protocol.union.integration.flow.service.FlowAuctionService
import com.rarible.protocol.union.integration.flow.service.FlowCollectionService
import com.rarible.protocol.union.integration.flow.service.FlowItemService
import com.rarible.protocol.union.integration.flow.service.FlowOrderService
import com.rarible.protocol.union.integration.flow.service.FlowOwnershipService
import com.rarible.protocol.union.integration.flow.service.FlowSignatureService
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import

@FlowConfiguration
@Import(CoreConfiguration::class)
@ComponentScan(basePackageClasses = [FlowOrderConverter::class])
@EnableConfigurationProperties(value = [FlowIntegrationProperties::class])
class FlowApiConfiguration(
    private val properties: FlowIntegrationProperties
) {

    private val flow = BlockchainDto.FLOW.name.lowercase()

    @Bean
    fun flowBlockchain(): BlockchainDto {
        return BlockchainDto.FLOW
    }

    //-------------------- API --------------------//

    @Bean
    fun flowNftIndexerApiClientFactory(
        uriProvider: FlowApiServiceUriProvider,
        webClientCustomizer: UnionWebClientCustomizer
    ): FlowNftIndexerApiClientFactory {
        return FlowNftIndexerApiClientFactory(uriProvider, webClientCustomizer)
    }

    @Bean
    fun flowItemApi(factory: FlowNftIndexerApiClientFactory): FlowNftItemControllerApi =
        factory.createNftItemApiClient()

    @Bean
    fun flowOwnershipApi(factory: FlowNftIndexerApiClientFactory): FlowNftOwnershipControllerApi =
        factory.createNftOwnershipApiClient()

    @Bean
    fun flowCollectionApi(factory: FlowNftIndexerApiClientFactory): FlowNftCollectionControllerApi =
        factory.createNftCollectionApiClient()

    @Bean
    fun flowOrderApi(factory: FlowNftIndexerApiClientFactory): FlowOrderControllerApi =
        factory.createNftOrderApiClient()

    @Bean
    fun flowBidApi(factory: FlowNftIndexerApiClientFactory): FlowBidOrderControllerApi =
        factory.createBidApiClient()

    @Bean
    fun flowActivityApi(factory: FlowNftIndexerApiClientFactory): FlowNftOrderActivityControllerApi =
        factory.createNftOrderActivityApiClient()

    @Bean
    fun flowCryptoApi(factory: FlowNftIndexerApiClientFactory): FlowNftCryptoControllerApi =
        factory.createCryptoApiClient()

    //-------------------- Services --------------------//

    @Bean
    fun flowItemService(controllerApi: FlowNftItemControllerApi): FlowItemService {
        return FlowItemService(controllerApi)
    }

    @Bean
    fun flowOwnershipService(controllerApi: FlowNftOwnershipControllerApi): FlowOwnershipService {
        return FlowOwnershipService(controllerApi)
    }

    @Bean
    fun flowCollectionService(controllerApi: FlowNftCollectionControllerApi): FlowCollectionService {
        return FlowCollectionService(controllerApi)
    }

    @Bean
    fun flowOrderService(
        orderApi: FlowOrderControllerApi,
        bidApi: FlowBidOrderControllerApi,
        converter: FlowOrderConverter
    ): OrderService {
        return OrderProxyService(
            FlowOrderService(orderApi, bidApi, converter),
            setOf(PlatformDto.RARIBLE)
        )
    }

    @Bean
    fun flowAuctionService(): AuctionService {
        return FlowAuctionService(BlockchainDto.FLOW)
    }

    @Bean
    fun flowSignatureService(controllerApi: FlowNftCryptoControllerApi): FlowSignatureService {
        return FlowSignatureService(controllerApi)
    }

    @Bean
    fun flowActivityService(
        activityApi: FlowNftOrderActivityControllerApi,
        converter: FlowActivityConverter
    ): FlowActivityService {
        return FlowActivityService(activityApi, converter)
    }
}
