package com.rarible.protocol.union.integration.tezos.dipdup

import com.apollographql.apollo3.ApolloClient
import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.dipdup.client.OrderActivityClient
import com.rarible.dipdup.client.OrderClient
import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupActivityConverter
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupOrderConverter
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
import com.rarible.tzkt.meta.MetaService
import com.rarible.tzkt.royalties.RoyaltiesHandler
import kotlinx.coroutines.runBlocking
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

@DipDupConfiguration
@Import(CoreConfiguration::class)
@ComponentScan(basePackageClasses = [DipDupOrderConverter::class])
@EnableConfigurationProperties(value = [DipDupIntegrationProperties::class])
class DipDupApiConfiguration(
    private val properties: DipDupIntegrationProperties
) {

    val apolloClient = runBlocking { ApolloClient.Builder().serverUrl(properties.dipdupUrl).build() }

    val tzktWebClient = webClient(properties.tzktUrl)
    val ipfsWebClient = webClient(properties.ipfsUrl)

    // Clients

    @Bean
    fun dipdupOrderApi() = OrderClient(apolloClient)

    @Bean
    fun dipdupOrderActivityApi() = OrderActivityClient(apolloClient)

    @Bean
    fun tzktBigMapKeyClient() = BigMapKeyClient(tzktWebClient)

    @Bean
    fun tzktCollectionClient() = CollectionClient(tzktWebClient)

    @Bean
    fun tzktIpfsClient(mapper: ObjectMapper) = IPFSClient(ipfsWebClient, mapper)

    @Bean
    fun tzktOwnershipClient() = OwnershipClient(tzktWebClient)

    @Bean
    fun tokenActivityClient() = TokenActivityClient(tzktWebClient)

    @Bean
    fun signatureClient() = SignatureClient(properties.nodeAddress, properties.chainId, properties.sigChecker)

    @Bean
    fun metaService(mapper: ObjectMapper, bigMapKeyClient: BigMapKeyClient) =
        MetaService(mapper, bigMapKeyClient, properties.knownAddresses!!)

    @Bean
    fun royaltyService(
        bigMapKeyClient: BigMapKeyClient,
        ipfsClient: IPFSClient
    ) = RoyaltiesHandler(bigMapKeyClient, ipfsClient, properties.knownAddresses!!)

    @Bean
    fun tokenClient(metaService: MetaService, royaltiesHandler: RoyaltiesHandler) =
        TokenClient(tzktWebClient, metaService, royaltiesHandler)

    // Services

    @Bean
    fun dipdupOrderService(orderClient: OrderClient, dipDupOrderConverter: DipDupOrderConverter): DipdupOrderService {
        return DipdupOrderServiceImpl(orderClient, dipDupOrderConverter)
    }

    @Bean
    fun dipdupOrderActivitiesService(
        orderActivityClient: OrderActivityClient,
        dipDupActivityConverter: DipDupActivityConverter
    ): DipdupOrderActivityService {
        return DipdupOrderActivityServiceImpl(orderActivityClient, dipDupActivityConverter)
    }

    @Bean
    fun tzktCollectionService(tzktClient: CollectionClient, tzktTokenClient: TokenClient, tezosCollectionRepository: TezosCollectionRepository): TzktCollectionService {
        return TzktCollectionServiceImpl(tzktClient, tzktTokenClient, tezosCollectionRepository)
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

    private fun webClient(url: String) = WebClient.builder()
        .exchangeStrategies(
            ExchangeStrategies.builder()
                .codecs { it.defaultCodecs().maxInMemorySize(10_000_000) }
                .build())
        .baseUrl(url)
        .build()

}
