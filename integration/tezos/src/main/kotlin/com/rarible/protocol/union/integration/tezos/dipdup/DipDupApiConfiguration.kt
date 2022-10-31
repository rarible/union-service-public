package com.rarible.protocol.union.integration.tezos.dipdup

import com.apollographql.apollo3.ApolloClient
import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.dipdup.client.OrderActivityClient
import com.rarible.dipdup.client.OrderClient
import com.rarible.dipdup.client.client.AuthorizationInterceptor
import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.integration.tezos.dipdup.client.TzktWebClientFactory
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupActivityConverter
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupOrderConverter
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipDupCollectionService
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipDupItemService
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipDupOwnershipService
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipDupRoyaltyService
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipDupTokenActivityService
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipdupOrderActivityService
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipdupOrderActivityServiceImpl
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipdupOrderService
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipdupOrderServiceImpl
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktCollectionService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktCollectionServiceImpl
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemActivityService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemActivityServiceImpl
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemServiceImpl
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktOwnershipService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktOwnershipServiceImpl
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktSignatureService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktSignatureServiceImpl
import com.rarible.protocol.union.integration.tezos.entity.TezosCollectionRepository
import com.rarible.tzkt.client.BigMapKeyClient
import com.rarible.tzkt.client.CollectionClient
import com.rarible.tzkt.client.IPFSClient
import com.rarible.tzkt.client.OwnershipClient
import com.rarible.tzkt.client.SignatureClient
import com.rarible.tzkt.client.TokenActivityClient
import com.rarible.tzkt.client.TokenClient
import com.rarible.tzkt.config.TzktSettings
import com.rarible.tzkt.meta.MetaCollectionService
import com.rarible.tzkt.meta.MetaService
import com.rarible.tzkt.royalties.RoyaltiesHandler
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.client.WebClient

@DipDupConfiguration
@Import(CoreConfiguration::class)
@ComponentScan(basePackageClasses = [DipDupOrderConverter::class])
@EnableConfigurationProperties(value = [DipDupIntegrationProperties::class])
class DipDupApiConfiguration(
    private val properties: DipDupIntegrationProperties,
    @Qualifier("unionDefaultWebClientCustomizer")
    private val unionDefaultWebClientCustomizer: WebClientCustomizer
) {

    val apolloClient = runBlocking {
        val builder = ApolloClient.Builder().serverUrl(properties.dipdupUrl)
        properties.dipdupToken?.let { builder.addHttpInterceptor(AuthorizationInterceptor(it)) }
        builder.build()
    }

    val tzktWebClient = webClient(properties.tzktUrl)
    val ipfsWebClient = webClient(properties.ipfsUrl)

    // Clients
    @Bean
    fun TzktSettings() = TzktSettings(
        useTokensBatch = properties.tzktProperties.tokenBatch,
        useOwnershipsBatch = properties.tzktProperties.ownershipBatch,
        useCollectionBatch = properties.tzktProperties.collectionBatch
    )

    @Bean
    fun dipdupOrderApi() = OrderClient(apolloClient)

    @Bean
    fun dipdupOrderActivityApi() = OrderActivityClient(apolloClient)

    @Bean
    fun dipdupTokenActivityApi() = com.rarible.dipdup.client.TokenActivityClient(apolloClient)

    @Bean
    fun dipdupTokenApi() = com.rarible.dipdup.client.TokenClient(apolloClient)

    @Bean
    fun dipdupRoyaltyApi() = com.rarible.dipdup.client.RoyaltiesClient(apolloClient)

    @Bean
    fun dipdupOwnershipApi() = com.rarible.dipdup.client.OwnershipClient(apolloClient)

    @Bean
    fun dipdupCollectionApi() = com.rarible.dipdup.client.CollectionClient(apolloClient)

    @Bean
    fun tzktBigMapKeyClient() = BigMapKeyClient(tzktWebClient)

    @Bean
    fun collectionMetaService(ipfsClient: IPFSClient) = MetaCollectionService(tzktWebClient, ipfsClient)

    @Bean
    fun tzktCollectionClient(
        metaCollectionService: MetaCollectionService, settings: TzktSettings
    ) = CollectionClient(tzktWebClient, metaCollectionService, settings)

    @Bean
    fun tzktIpfsClient(mapper: ObjectMapper) = IPFSClient(ipfsWebClient, mapper)

    @Bean
    fun tzktOwnershipClient(settings: TzktSettings) = OwnershipClient(tzktWebClient, settings)

    @Bean
    fun tokenActivityClient() = TokenActivityClient(tzktWebClient)

    @Bean
    fun signatureClient() = SignatureClient(properties.nodeAddress, properties.chainId, properties.sigChecker)

    @Bean
    fun metaService(mapper: ObjectMapper, ipfsClient: IPFSClient, bigMapKeyClient: BigMapKeyClient) =
        MetaService(mapper, bigMapKeyClient, ipfsClient, properties.knownAddresses!!)

    @Bean
    fun royaltyService(
        bigMapKeyClient: BigMapKeyClient,
        ownershipClient: OwnershipClient,
        ipfsClient: IPFSClient
    ) = RoyaltiesHandler(bigMapKeyClient, ownershipClient, ipfsClient, properties.knownAddresses!!)

    @Bean
    fun tokenClient(metaService: MetaService, royaltiesHandler: RoyaltiesHandler, settings: TzktSettings) =
        TokenClient(tzktWebClient, metaService, royaltiesHandler, settings)

    // Services

    @Bean
    fun dipdupOrderService(orderClient: OrderClient, dipDupOrderConverter: DipDupOrderConverter): DipdupOrderService {
        return DipdupOrderServiceImpl(orderClient, dipDupOrderConverter, properties.marketplaces)
    }

    @Bean
    fun dipdupOrderActivitiesService(
        orderActivityClient: OrderActivityClient,
        dipDupActivityConverter: DipDupActivityConverter
    ): DipdupOrderActivityService {
        return DipdupOrderActivityServiceImpl(orderActivityClient, dipDupActivityConverter)
    }

    @Bean
    fun dipdupTokenActivitiesService(
        dipDupTokenActivityClient: com.rarible.dipdup.client.TokenActivityClient,
        dipDupActivityConverter: DipDupActivityConverter
    ): DipDupTokenActivityService {
        return DipDupTokenActivityService(dipDupTokenActivityClient, dipDupActivityConverter)
    }

    @Bean
    fun dipdupItemService(tokenClient: com.rarible.dipdup.client.TokenClient): DipDupItemService {
        return DipDupItemService(tokenClient)
    }

    @Bean
    fun dipdupRoyaltiyService(royltiesClient: com.rarible.dipdup.client.RoyaltiesClient): DipDupRoyaltyService {
        return DipDupRoyaltyService(royltiesClient)
    }

    @Bean
    fun dipdupOwnershipService(ownershipClient: com.rarible.dipdup.client.OwnershipClient): DipDupOwnershipService {
        return DipDupOwnershipService(ownershipClient)
    }

    @Bean
    fun dipdupCollectionService(collectionClient: com.rarible.dipdup.client.CollectionClient): DipDupCollectionService {
        return DipDupCollectionService(collectionClient)
    }

    @Bean
    fun tzktCollectionService(tzktClient: CollectionClient, tzktTokenClient: TokenClient, tezosCollectionRepository: TezosCollectionRepository): TzktCollectionService {
        return TzktCollectionServiceImpl(tzktClient, tzktTokenClient, tezosCollectionRepository, properties.tzktProperties)
    }

    @Bean
    fun tzktItemService(tzktTokenClient: TokenClient): TzktItemService {
        return TzktItemServiceImpl(tzktTokenClient, properties)
    }

    @Bean
    fun tzktOwnershipService(ownershipClient: OwnershipClient): TzktOwnershipService {
        return TzktOwnershipServiceImpl(ownershipClient)
    }

    @Bean
    fun tzktItemActivityService(tokenActivityClient: TokenActivityClient): TzktItemActivityService {
        return TzktItemActivityServiceImpl(tokenActivityClient)
    }

    @Bean
    fun tzktSignatureService(signatureClient: SignatureClient): TzktSignatureService {
        return TzktSignatureServiceImpl(signatureClient)
    }

    private fun webClient(url: String): WebClient {
        val builder = TzktWebClientFactory.createClient(url)
        unionDefaultWebClientCustomizer.customize(builder)
        return builder.build()
    }

}
