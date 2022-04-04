package com.rarible.protocol.union.integration.tezos.dipdup

import com.apollographql.apollo3.ApolloClient
import com.rarible.dipdup.client.ActivityClient
import com.rarible.dipdup.client.OrderClient
import com.rarible.protocol.tezos.api.client.NftItemControllerApi
import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.integration.tezos.TezosIntegrationProperties
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupOrderConverter
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipdupOrderService
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipdupOrderServiceImpl
import com.rarible.protocol.union.integration.tezos.service.TezosItemService
import kotlinx.coroutines.runBlocking
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import

@DipDupConfiguration
@Import(CoreConfiguration::class)
@ComponentScan(basePackageClasses = [DipDupOrderConverter::class])
@EnableConfigurationProperties(value = [DipDupIntegrationProperties::class])
class DipDupApiConfiguration(
    private val properties: DipDupIntegrationProperties
) {

    val apolloClient = runBlocking { ApolloClient.Builder().serverUrl(properties.dipdupUrl).build() }

    @Bean
    fun dipdupOrderApi() = OrderClient(apolloClient)

    @Bean
    fun dipdupOrderActivityApi() = ActivityClient(apolloClient)

    // Services

    @Bean
    fun dipdupOrderService(orderClient: OrderClient, dipDupOrderConverter: DipDupOrderConverter): DipdupOrderService {
        return DipdupOrderServiceImpl(orderClient, dipDupOrderConverter)
    }

}
