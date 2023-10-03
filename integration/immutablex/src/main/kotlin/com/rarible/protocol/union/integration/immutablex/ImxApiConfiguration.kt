package com.rarible.protocol.union.integration.immutablex

import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.core.UnionWebClientCustomizer
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.immutablex.client.ImxActivityClient
import com.rarible.protocol.union.integration.immutablex.client.ImxAssetClient
import com.rarible.protocol.union.integration.immutablex.client.ImxCollectionClient
import com.rarible.protocol.union.integration.immutablex.client.ImxOrderClient
import com.rarible.protocol.union.integration.immutablex.client.ImxWebClientFactory
import com.rarible.protocol.union.integration.immutablex.converter.ImxActivityConverter
import com.rarible.protocol.union.integration.immutablex.converter.ImxOrderConverter
import com.rarible.protocol.union.integration.immutablex.converter.ImxOrderV3Converter
import com.rarible.protocol.union.integration.immutablex.repository.ImxCollectionCreatorRepository
import com.rarible.protocol.union.integration.immutablex.repository.ImxCollectionMetaSchemaRepository
import com.rarible.protocol.union.integration.immutablex.repository.ImxItemMetaRepository
import com.rarible.protocol.union.integration.immutablex.scanner.ImxEventsApi
import com.rarible.protocol.union.integration.immutablex.service.ImxActivityService
import com.rarible.protocol.union.integration.immutablex.service.ImxCollectionService
import com.rarible.protocol.union.integration.immutablex.service.ImxItemService
import com.rarible.protocol.union.integration.immutablex.service.ImxOrderService
import com.rarible.protocol.union.integration.immutablex.service.ImxOwnershipService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.web.reactive.function.client.WebClient

@ImxConfiguration
@Import(CoreConfiguration::class)
@ComponentScan(basePackageClasses = [ImxActivityConverter::class])
@EnableConfigurationProperties(ImxIntegrationProperties::class)
class ImxApiConfiguration {

    @Bean
    fun imxBlockchain() = BlockchainDto.IMMUTABLEX

    @Bean
    fun imxFeatureFlags(props: ImxIntegrationProperties) = props.featureFlags

    @Bean
    fun imxClientProperties(props: ImxIntegrationProperties): ImxClientProperties {
        return props.client as ImxClientProperties
    }

    @Bean
    @Qualifier("imxWebClient")
    fun imxWebClient(
        props: ImxIntegrationProperties,
        webClientCustomizer: UnionWebClientCustomizer
    ): WebClient {
        val builder = ImxWebClientFactory.configureClient(props.client!!.url!!, props.apiKey)
        webClientCustomizer.customize(builder)
        return builder.build()
    }

    @Bean
    @Qualifier("imxWebClientV3")
    fun imxWebClientV3(
        props: ImxIntegrationProperties,
        webClientCustomizer: UnionWebClientCustomizer
    ): WebClient {
        val builder = ImxWebClientFactory.configureClient(props.clientV3!!.url!!, props.apiKey)
        webClientCustomizer.customize(builder)
        return builder.build()
    }

    @Bean
    fun imxAssetClient(
        @Qualifier("imxWebClient")
        imxWebClient: WebClient,
        clientProperties: ImxClientProperties
    ) = ImxAssetClient(imxWebClient, clientProperties.byIdsChunkSize)

    @Bean
    fun imxActivityClient(
        @Qualifier("imxWebClientV3")
        imxWebClient: WebClient,
        clientProperties: ImxClientProperties
    ) = ImxActivityClient(imxWebClient, clientProperties.byIdsChunkSize)

    @Bean
    fun imxCollectionClient(
        @Qualifier("imxWebClient")
        imxWebClient: WebClient,
        clientProperties: ImxClientProperties
    ) = ImxCollectionClient(imxWebClient, clientProperties.byIdsChunkSize)

    @Bean
    fun imxOrderClient(
        @Qualifier("imxWebClientV3")
        imxWebClient: WebClient,
        clientProperties: ImxClientProperties
    ) = ImxOrderClient(imxWebClient, clientProperties.byIdsChunkSize)

    @Bean
    fun imxItemService(
        assetClient: ImxAssetClient,
        activityClient: ImxActivityClient,
        collectionClient: ImxCollectionClient,
        collectionCreatorRepository: ImxCollectionCreatorRepository,
        collectionMetaSchemaRepository: ImxCollectionMetaSchemaRepository
    ): ImxItemService {
        return ImxItemService(
            assetClient,
            activityClient,
            collectionClient,
            collectionCreatorRepository,
            collectionMetaSchemaRepository
        )
    }

    @Bean
    fun imxCollectionCreatorRepository(
        mongo: ReactiveMongoTemplate
    ): ImxCollectionCreatorRepository {
        return ImxCollectionCreatorRepository(mongo)
    }

    @Bean
    fun imxCollectionMetaSchemaRepository(
        mongo: ReactiveMongoTemplate
    ): ImxCollectionMetaSchemaRepository {
        return ImxCollectionMetaSchemaRepository(mongo)
    }

    @Bean
    fun imxItemMetaRepository(
        mongo: ReactiveMongoTemplate
    ): ImxItemMetaRepository {
        return ImxItemMetaRepository(mongo)
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
        featureFlags: ImxFeatureFlags,
        orderClient: ImxOrderClient,
        orderClientV3: ImxOrderClient,
        imxOrderConverter: ImxOrderConverter
    ): ImxOrderService {
        val client = when (featureFlags.useOrderV3) {
            true -> orderClientV3
            else -> orderClient
        }
        return ImxOrderService(client, imxOrderConverter)
    }

    @Bean
    fun imxOrderConverter(
        featureFlags: ImxFeatureFlags,
    ) = when (featureFlags.useOrderV3) {
        true -> ImxOrderConverter()
        else -> ImxOrderV3Converter()
    }

    @Bean
    fun imxActivityService(
        activityClient: ImxActivityClient,
        orderClient: ImxOrderClient,
        converter: ImxActivityConverter
    ): ImxActivityService {
        return ImxActivityService(activityClient, orderClient, converter)
    }

    @Bean
    fun imxEventsApi(
        assetClient: ImxAssetClient,
        activityClient: ImxActivityClient,
        orderClient: ImxOrderClient,
        collectionClient: ImxCollectionClient
    ) = ImxEventsApi(activityClient, assetClient, orderClient, collectionClient)
}
