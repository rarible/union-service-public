package com.rarible.protocol.union.core

import com.rarible.protocol.flow.nft.api.client.FlowNftCollectionControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftItemControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftOwnershipControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowOrderControllerApi
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.union.core.ethereum.service.EthereumCollectionService
import com.rarible.protocol.union.core.ethereum.service.EthereumItemService
import com.rarible.protocol.union.core.ethereum.service.EthereumOrderService
import com.rarible.protocol.union.core.ethereum.service.EthereumOwnershipService
import com.rarible.protocol.union.core.flow.service.FlowCollectionService
import com.rarible.protocol.union.core.flow.service.FlowItemService
import com.rarible.protocol.union.core.flow.service.FlowOrderService
import com.rarible.protocol.union.core.flow.service.FlowOwnershipService
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.OwnershipService
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

}