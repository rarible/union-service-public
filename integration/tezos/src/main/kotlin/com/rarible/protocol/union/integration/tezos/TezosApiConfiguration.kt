package com.rarible.protocol.union.integration.tezos

import com.rarible.protocol.tezos.api.client.FixedTezosApiServiceUriProvider
import com.rarible.protocol.tezos.api.client.NftActivityControllerApi
import com.rarible.protocol.tezos.api.client.NftCollectionControllerApi
import com.rarible.protocol.tezos.api.client.NftItemControllerApi
import com.rarible.protocol.tezos.api.client.NftOwnershipControllerApi
import com.rarible.protocol.tezos.api.client.OrderActivityControllerApi
import com.rarible.protocol.tezos.api.client.OrderControllerApi
import com.rarible.protocol.tezos.api.client.OrderSignatureControllerApi
import com.rarible.protocol.tezos.api.client.TezosApiClientFactory
import com.rarible.protocol.tezos.api.client.TezosApiServiceUriProvider
import com.rarible.protocol.union.core.CoreConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.net.URI

@Configuration
@TezosComponent
@Import(CoreConfiguration::class)
@ComponentScan(basePackageClasses = [TezosApiConfiguration::class])
@EnableConfigurationProperties(value = [TezosIntegrationProperties::class])
class TezosApiConfiguration(
    private val properties: TezosIntegrationProperties
) {

    @Bean
    fun tezosFixedApiServiceUriProvider(): TezosApiServiceUriProvider {
        return FixedTezosApiServiceUriProvider(URI(properties.client!!.url!!))
    }

    @Bean
    fun tezosItemApi(factory: TezosApiClientFactory): NftItemControllerApi =
        factory.createNftItemApiClient()

    @Bean
    fun tezosOwnershipApi(factory: TezosApiClientFactory): NftOwnershipControllerApi =
        factory.createNftOwnershipApiClient()

    @Bean
    fun tezosCollectionApi(factory: TezosApiClientFactory): NftCollectionControllerApi =
        factory.createNftCollectionApiClient()

    @Bean
    fun tezosOrderApi(factory: TezosApiClientFactory): OrderControllerApi =
        factory.createOrderApiClient()

    @Bean
    fun tezosSignatureApi(factory: TezosApiClientFactory): OrderSignatureControllerApi =
        factory.createOrderSignatureApiClient()

    @Bean
    fun tezosNftActivityApi(factory: TezosApiClientFactory): OrderActivityControllerApi =
        factory.createOrderActivityApiClient()

    @Bean
    fun tezosOrderActivityApi(factory: TezosApiClientFactory): NftActivityControllerApi =
        factory.createNftActivityApiClient()
}