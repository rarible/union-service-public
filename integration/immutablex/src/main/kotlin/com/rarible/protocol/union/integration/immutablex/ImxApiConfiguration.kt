package com.rarible.protocol.union.integration.immutablex

import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.immutablex.client.ImxActivityClient
import com.rarible.protocol.union.integration.immutablex.client.ImxAssetClient
import com.rarible.protocol.union.integration.immutablex.client.ImxCollectionClient
import com.rarible.protocol.union.integration.immutablex.client.ImxOrderClient
import com.rarible.protocol.union.integration.immutablex.client.ImxWebClientFactory
import com.rarible.protocol.union.integration.immutablex.scanner.ImxEventsApi
import com.rarible.protocol.union.integration.immutablex.service.ImxActivityService
import com.rarible.protocol.union.integration.immutablex.service.ImxCollectionService
import com.rarible.protocol.union.integration.immutablex.service.ImxItemService
import com.rarible.protocol.union.integration.immutablex.service.ImxOrderService
import com.rarible.protocol.union.integration.immutablex.service.ImxOwnershipService
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.client.WebClient

@ImxConfiguration
@Import(CoreConfiguration::class)
@EnableConfigurationProperties(ImxIntegrationProperties::class)
class ImxApiConfiguration {

    @Bean
    fun imxBlockchain() = BlockchainDto.IMMUTABLEX

    @Bean
    fun imxWebClient(props: ImxIntegrationProperties): WebClient {
        return ImxWebClientFactory.createClient(props.client!!.url!!, props.apiKey)
    }

    @Bean
    fun imxAssetClient(immutablexWebClient: WebClient) = ImxAssetClient(immutablexWebClient)

    @Bean
    fun imxActivityClient(immutablexWebClient: WebClient) = ImxActivityClient(immutablexWebClient)

    @Bean
    fun imxCollectionClient(immutablexWebClient: WebClient) = ImxCollectionClient(immutablexWebClient)

    @Bean
    fun imxOrderClient(immutablexWebClient: WebClient) = ImxOrderClient(immutablexWebClient)

    @Bean
    fun immutablexItemService(
        assetClient: ImxAssetClient,
        activityClient: ImxActivityClient
    ): ImxItemService {
        return ImxItemService(assetClient, activityClient)
    }

    @Bean
    fun immutablexOwnershipService(
        assetClient: ImxAssetClient,
        activityClient: ImxActivityClient
    ): ImxOwnershipService {
        return ImxOwnershipService(assetClient, activityClient)
    }

    @Bean
    fun immutablexCollectionService(
        collectionClient: ImxCollectionClient
    ): ImxCollectionService {
        return ImxCollectionService(collectionClient)
    }

    @Bean
    fun immutablexOrderService(
        orderClient: ImxOrderClient
    ): ImxOrderService {
        return ImxOrderService(orderClient)
    }

    @Bean
    fun immutablesActivityService(
        activityClient: ImxActivityClient,
        orderService: ImxOrderService
    ): ImxActivityService {
        return ImxActivityService(activityClient, orderService)
    }

    @Bean
    fun eventsApi(
        assetClient: ImxAssetClient,
        activityClient: ImxActivityClient,
        orderClient: ImxOrderClient,
        collectionClient: ImxCollectionClient
    ) = ImxEventsApi(activityClient, assetClient, orderClient, collectionClient)
}
