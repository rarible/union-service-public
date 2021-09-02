package com.rarible.protocol.union.api.configuration

import com.rarible.protocol.flow.nft.api.client.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FlowApiClientConfiguration {

    @Bean
    fun flowNftItemApi(): FlowNftItemControllerApi = FlowNftItemControllerApi()

    @Bean
    fun flowNftOwnershipApi(): FlowNftOwnershipControllerApi = FlowNftOwnershipControllerApi()

    @Bean
    fun flowNftCollectionApi(): FlowNftCollectionControllerApi = FlowNftCollectionControllerApi()

    @Bean
    fun flowOrderApi(): FlowOrderControllerApi = FlowOrderControllerApi()

    @Bean
    fun flowOrderActivityApi(): FlowNftOrderActivityControllerApi = FlowNftOrderActivityControllerApi()

}
