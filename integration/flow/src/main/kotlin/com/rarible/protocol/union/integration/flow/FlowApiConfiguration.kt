package com.rarible.protocol.union.integration.flow

import com.rarible.protocol.flow.nft.api.client.FlowNftCollectionControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftCryptoControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftIndexerApiClientFactory
import com.rarible.protocol.flow.nft.api.client.FlowNftItemControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftOrderActivityControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftOwnershipControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowOrderControllerApi
import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@FlowComponent
@Import(CoreConfiguration::class)
@ComponentScan(basePackageClasses = [FlowApiConfiguration::class])
@EnableConfigurationProperties(value = [FlowIntegrationProperties::class])
class FlowApiConfiguration(
    private val properties: FlowIntegrationProperties
) {

    private val flow = BlockchainDto.FLOW.name.toLowerCase()

    @Bean
    fun flowItemApi(factory: FlowNftIndexerApiClientFactory): FlowNftItemControllerApi =
        factory.createNftItemApiClient(flow)

    @Bean
    fun flowOwnershipApi(factory: FlowNftIndexerApiClientFactory): FlowNftOwnershipControllerApi =
        factory.createNftOwnershipApiClient(flow)

    @Bean
    fun flowCollectionApi(factory: FlowNftIndexerApiClientFactory): FlowNftCollectionControllerApi =
        factory.createNftCollectionApiClient(flow)

    @Bean
    fun flowOrderApi(factory: FlowNftIndexerApiClientFactory): FlowOrderControllerApi =
        factory.createNftOrderApiClient(flow)

    @Bean
    fun flowActivityApi(factory: FlowNftIndexerApiClientFactory): FlowNftOrderActivityControllerApi =
        factory.createNftOrderActivityApiClient(flow)

    @Bean
    fun flowCryptoApi(factory: FlowNftIndexerApiClientFactory): FlowNftCryptoControllerApi =
        factory.createCryptoApiClient(flow)
}