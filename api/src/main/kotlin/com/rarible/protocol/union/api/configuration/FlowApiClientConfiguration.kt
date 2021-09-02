package com.rarible.protocol.union.api.configuration

import com.rarible.protocol.flow.nft.api.client.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FlowApiClientConfiguration {

    private val blockchain = "Flow"

    @Bean
    fun flowNftItemApi(factory: FlowNftIndexerApiClientFactory): FlowNftItemControllerApi =
        factory.createNftItemApiClient(blockchain)

    @Bean
    fun flowNftOwnershipApi(factory: FlowNftIndexerApiClientFactory): FlowNftOwnershipControllerApi =
        factory.createNftOwnershipApiClient(blockchain)

    @Bean
    fun flowNftCollectionApi(factory: FlowNftIndexerApiClientFactory): FlowNftCollectionControllerApi =
        factory.createNftCollectionApiClient(blockchain)

    @Bean
    fun flowOrderApi(factory: FlowNftIndexerApiClientFactory): FlowOrderControllerApi =
        factory.createNftOrderApiClient(blockchain)

    @Bean
    fun flowOrderActivityApi(factory: FlowNftIndexerApiClientFactory): FlowNftOrderActivityControllerApi =
        factory.createNftOrderActivityApiClient(blockchain)

}
