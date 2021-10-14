package com.rarible.protocol.union.core.flow

import com.rarible.protocol.flow.nft.api.client.FlowNftCollectionControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftIndexerApiClientFactory
import com.rarible.protocol.flow.nft.api.client.FlowNftItemControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftOrderActivityControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftOwnershipControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowOrderControllerApi
import com.rarible.protocol.union.core.flow.converter.FlowOrderConverter
import com.rarible.protocol.union.core.flow.service.FlowActivityService
import com.rarible.protocol.union.core.flow.service.FlowCollectionService
import com.rarible.protocol.union.core.flow.service.FlowItemService
import com.rarible.protocol.union.core.flow.service.FlowOrderService
import com.rarible.protocol.union.core.flow.service.FlowOwnershipService
import com.rarible.protocol.union.core.flow.service.FlowSignatureService
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FlowConfiguration {

    private val flow = BlockchainDto.FLOW.name.toLowerCase()

    //--------------------- FLOW ---------------------//
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

    //---------------------- FLOW SERVICES -----------------------//
    @Bean
    fun flowItemService(flowItemApi: FlowNftItemControllerApi): ItemService {
        return FlowItemService(BlockchainDto.FLOW, flowItemApi)
    }

    @Bean
    fun flowOwnershipService(flowOwnershipApi: FlowNftOwnershipControllerApi): OwnershipService {
        return FlowOwnershipService(BlockchainDto.FLOW, flowOwnershipApi)
    }

    @Bean
    fun flowCollectionService(flowCollectionApi: FlowNftCollectionControllerApi): CollectionService {
        return FlowCollectionService(BlockchainDto.FLOW, flowCollectionApi)
    }

    @Bean
    fun flowOrderService(flowOrderApi: FlowOrderControllerApi, flowOrderConverter: FlowOrderConverter): OrderService {
        return FlowOrderService(BlockchainDto.FLOW, flowOrderApi, flowOrderConverter)
    }

    @Bean
    fun flowSignatureService(flowOrderApi: FlowOrderControllerApi): SignatureService {
        return FlowSignatureService(BlockchainDto.FLOW) // TODO implement it later
    }

    @Bean
    fun flowActivityService(flowActivityApi: FlowNftOrderActivityControllerApi): ActivityService {
        return FlowActivityService(BlockchainDto.FLOW, flowActivityApi)
    }

}