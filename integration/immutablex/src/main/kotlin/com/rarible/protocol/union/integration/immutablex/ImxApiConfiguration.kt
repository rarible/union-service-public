package com.rarible.protocol.union.integration.immutablex

import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.immutablex.cache.ImxCollectionCreatorRepository
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
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.web.reactive.function.client.WebClient

@ImxConfiguration
@Import(CoreConfiguration::class)
@EnableConfigurationProperties(ImxIntegrationProperties::class)
class ImxApiConfiguration {

    @Bean
    fun imxBlockchain() = BlockchainDto.IMMUTABLEX

    @Bean
    @Qualifier("imxWebClient")
    fun imxWebClient(props: ImxIntegrationProperties): WebClient {
        return ImxWebClientFactory.createClient(props.client!!.url!!, props.apiKey)
    }

    @Bean
    fun imxAssetClient(
        @Qualifier("imxWebClient")
        imxWebClient: WebClient
    ) = ImxAssetClient(imxWebClient)

    @Bean
    fun imxActivityClient(
        @Qualifier("imxWebClient")
        imxWebClient: WebClient
    ) = ImxActivityClient(imxWebClient)

    @Bean
    fun imxCollectionClient(
        @Qualifier("imxWebClient")
        imxWebClient: WebClient
    ) = ImxCollectionClient(imxWebClient)

    @Bean
    fun imxOrderClient(
        @Qualifier("imxWebClient")
        imxWebClient: WebClient
    ) = ImxOrderClient(imxWebClient)

    @Bean
    fun imxItemService(
        assetClient: ImxAssetClient,
        activityClient: ImxActivityClient,
        collectionClient: ImxCollectionClient,
        collectionCreatorRepository: ImxCollectionCreatorRepository
    ): ImxItemService {
        return ImxItemService(assetClient, activityClient, collectionClient, collectionCreatorRepository)
    }

    @Bean
    fun imxCollectionCreatorRepository(
        mongo: ReactiveMongoTemplate
    ): ImxCollectionCreatorRepository {
        return ImxCollectionCreatorRepository(mongo)
    }

    @Bean
    fun imxOwnershipService(
        assetClient: ImxAssetClient,
        itemService: ImxItemService
    ): ImxOwnershipService {
        return ImxOwnershipService(assetClient, itemService)
    }

    @Bean
    fun imxCollectionService(
        collectionClient: ImxCollectionClient
    ): ImxCollectionService {
        return ImxCollectionService(collectionClient)
    }

    @Bean
    fun imxOrderService(
        orderClient: ImxOrderClient
    ): ImxOrderService {
        return ImxOrderService(orderClient)
    }

    @Bean
    fun imxActivityService(
        activityClient: ImxActivityClient,
        orderClient: ImxOrderClient
    ): ImxActivityService {
        return ImxActivityService(activityClient, orderClient)
    }

    @Bean
    fun imxEventsApi(
        assetClient: ImxAssetClient,
        activityClient: ImxActivityClient,
        orderClient: ImxOrderClient,
        collectionClient: ImxCollectionClient
    ) = ImxEventsApi(activityClient, assetClient, orderClient, collectionClient)
}
