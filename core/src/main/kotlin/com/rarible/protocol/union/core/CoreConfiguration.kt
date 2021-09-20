package com.rarible.protocol.union.core

import com.rarible.protocol.flow.nft.api.client.*
import com.rarible.protocol.nft.api.client.NftActivityControllerApi
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.order.api.client.OrderActivityControllerApi
import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.order.api.client.OrderSignatureControllerApi
import com.rarible.protocol.union.core.ethereum.service.*
import com.rarible.protocol.union.core.flow.service.*
import com.rarible.protocol.union.core.service.*
import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.dto.FlowBlockchainDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackageClasses = [CoreConfiguration::class])
class CoreConfiguration {

    //--------------------- ETHEREUM --------------------//
    @Bean
    fun ethereumItemService(@Qualifier("ethereum.item.api") ethereumItemApi: NftItemControllerApi): ItemService {
        return EthereumItemService(EthBlockchainDto.ETHEREUM, ethereumItemApi)
    }

    @Bean
    fun ethereumOwnershipService(@Qualifier("ethereum.ownership.api") ethereumOwnershipApi: NftOwnershipControllerApi): OwnershipService {
        return EthereumOwnershipService(EthBlockchainDto.ETHEREUM, ethereumOwnershipApi)
    }

    @Bean
    fun ethereumCollectionService(@Qualifier("ethereum.collection.api") ethereumCollectionApi: NftCollectionControllerApi): CollectionService {
        return EthereumCollectionService(EthBlockchainDto.ETHEREUM, ethereumCollectionApi)
    }

    @Bean
    fun ethereumOrderService(@Qualifier("ethereum.order.api") ethereumOrderApi: OrderControllerApi): OrderService {
        return EthereumOrderService(EthBlockchainDto.ETHEREUM, ethereumOrderApi)
    }

    @Bean
    fun ethereumSignatureService(@Qualifier("ethereum.signature.api") ethereumSignatureApi: OrderSignatureControllerApi): SignatureService {
        return EthereumSignatureService(EthBlockchainDto.ETHEREUM, ethereumSignatureApi)
    }

    @Bean
    fun ethereumActivityService(
        @Qualifier("ethereum.activity.api.item") ethereumActivityItemApi: NftActivityControllerApi,
        @Qualifier("ethereum.activity.api.order") ethereumActivityOrderApi: OrderActivityControllerApi
    ): ActivityService {
        return EthereumActivityService(EthBlockchainDto.ETHEREUM, ethereumActivityItemApi, ethereumActivityOrderApi)
    }

    //--------------------- POLYGON ---------------------//
    @Bean
    fun polygonItemService(@Qualifier("polygon.item.api") ethereumItemApi: NftItemControllerApi): ItemService {
        return EthereumItemService(EthBlockchainDto.POLYGON, ethereumItemApi)
    }

    @Bean
    fun polygonOwnershipService(@Qualifier("polygon.ownership.api") ethereumOwnershipApi: NftOwnershipControllerApi): OwnershipService {
        return EthereumOwnershipService(EthBlockchainDto.POLYGON, ethereumOwnershipApi)
    }

    @Bean
    fun polygonCollectionService(@Qualifier("polygon.collection.api") ethereumCollectionApi: NftCollectionControllerApi): CollectionService {
        return EthereumCollectionService(EthBlockchainDto.POLYGON, ethereumCollectionApi)
    }

    @Bean
    fun polygonOrderService(@Qualifier("polygon.order.api") ethereumOrderApi: OrderControllerApi): OrderService {
        return EthereumOrderService(EthBlockchainDto.POLYGON, ethereumOrderApi)
    }

    @Bean
    fun polygonSignatureService(@Qualifier("polygon.signature.api") ethereumSignatureApi: OrderSignatureControllerApi): SignatureService {
        return EthereumSignatureService(EthBlockchainDto.POLYGON, ethereumSignatureApi)
    }

    @Bean
    fun polygonActivityService(
        @Qualifier("polygon.activity.api.item") polygonActivityItemApi: NftActivityControllerApi,
        @Qualifier("polygon.activity.api.order") polygonActivityOrderApi: OrderActivityControllerApi
    ): ActivityService {
        return EthereumActivityService(EthBlockchainDto.POLYGON, polygonActivityItemApi, polygonActivityOrderApi)
    }

    //---------------------- FLOW -----------------------//
    @Bean
    fun flowItemService(flowItemApi: FlowNftItemControllerApi): ItemService {
        return FlowItemService(FlowBlockchainDto.FLOW, flowItemApi)
    }

    @Bean
    fun flowOwnershipService(flowOwnershipApi: FlowNftOwnershipControllerApi): OwnershipService {
        return FlowOwnershipService(FlowBlockchainDto.FLOW, flowOwnershipApi)
    }

    @Bean
    fun flowCollectionService(flowCollectionApi: FlowNftCollectionControllerApi): CollectionService {
        return FlowCollectionService(FlowBlockchainDto.FLOW, flowCollectionApi)
    }

    @Bean
    fun flowOrderService(flowOrderApi: FlowOrderControllerApi): OrderService {
        return FlowOrderService(FlowBlockchainDto.FLOW, flowOrderApi)
    }

    @Bean
    fun flowSignatureService(flowOrderApi: FlowOrderControllerApi): SignatureService {
        return FlowSignatureService(FlowBlockchainDto.FLOW) // TODO implement it later
    }

    @Bean
    fun flowActivityService(flowActivityApi: FlowNftOrderActivityControllerApi): ActivityService {
        return FlowActivityService(FlowBlockchainDto.FLOW, flowActivityApi)
    }

}