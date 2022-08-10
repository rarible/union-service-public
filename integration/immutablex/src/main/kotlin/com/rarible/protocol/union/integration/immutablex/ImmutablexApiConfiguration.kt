package com.rarible.protocol.union.integration.immutablex

import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexActivityClient
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexAssetClient
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexCollectionClient
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrderClient
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexWebClientFactory
import com.rarible.protocol.union.integration.immutablex.scanner.ImxEventsApi
import com.rarible.protocol.union.integration.immutablex.service.ImmutablexActivityService
import com.rarible.protocol.union.integration.immutablex.service.ImmutablexCollectionService
import com.rarible.protocol.union.integration.immutablex.service.ImmutablexItemService
import com.rarible.protocol.union.integration.immutablex.service.ImmutablexOrderService
import com.rarible.protocol.union.integration.immutablex.service.ImmutablexOwnershipService
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.client.WebClient

@ImmutablexConfiguration
@Import(CoreConfiguration::class)
@EnableConfigurationProperties(ImmutablexIntegrationProperties::class)
class ImmutablexApiConfiguration {

    @Bean
    fun immutablexBlockchain() = BlockchainDto.IMMUTABLEX

    @Bean
    fun immutablexWebClient(props: ImmutablexIntegrationProperties): WebClient {
        return ImmutablexWebClientFactory.createClient(props.client!!.url!!, props.apiKey)
    }

    @Bean
    fun immutablexAssetClient(immutablexWebClient: WebClient) = ImmutablexAssetClient(immutablexWebClient)

    @Bean
    fun immutablexActivityClient(immutablexWebClient: WebClient) = ImmutablexActivityClient(immutablexWebClient)

    @Bean
    fun immutablexCollectionClient(immutablexWebClient: WebClient) = ImmutablexCollectionClient(immutablexWebClient)

    @Bean
    fun immutablexOrderClient(immutablexWebClient: WebClient) = ImmutablexOrderClient(immutablexWebClient)

    @Bean
    fun immutablexItemService(
        assetClient: ImmutablexAssetClient,
        activityClient: ImmutablexActivityClient
    ): ImmutablexItemService {
        return ImmutablexItemService(assetClient, activityClient)
    }

    @Bean
    fun immutablexOwnershipService(
        assetClient: ImmutablexAssetClient,
        activityClient: ImmutablexActivityClient
    ): ImmutablexOwnershipService {
        return ImmutablexOwnershipService(assetClient, activityClient)
    }

    @Bean
    fun immutablexCollectionService(
        collectionClient: ImmutablexCollectionClient
    ): ImmutablexCollectionService {
        return ImmutablexCollectionService(collectionClient)
    }

    @Bean
    fun immutablexOrderService(
        orderClient: ImmutablexOrderClient
    ): ImmutablexOrderService {
        return ImmutablexOrderService(orderClient)
    }

    @Bean
    fun immutablesActivityService(
        activityClient: ImmutablexActivityClient,
        orderService: ImmutablexOrderService
    ): ImmutablexActivityService {
        return ImmutablexActivityService(activityClient, orderService)
    }

    @Bean
    fun eventsApi(
        activityClient: ImmutablexActivityClient,
        orderClient: ImmutablexOrderClient
    ) = ImxEventsApi(activityClient, orderClient)
}
