package com.rarible.protocol.union.core

import com.rarible.protocol.flow.nft.api.client.FlowNftCollectionControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftItemControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftOwnershipControllerApi
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.union.core.ethereum.service.EthereumCollectionService
import com.rarible.protocol.union.core.ethereum.service.EthereumItemService
import com.rarible.protocol.union.core.ethereum.service.EthereumOwnershipService
import com.rarible.protocol.union.core.flow.service.FlowCollectionService
import com.rarible.protocol.union.core.flow.service.FlowItemService
import com.rarible.protocol.union.core.flow.service.FlowOwnershipService
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.ItemService
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
    fun ethereumItemService(@Qualifier("ethereum.item.api") ethereumNftItemApi: NftItemControllerApi): ItemService {
        return EthereumItemService(EthBlockchainDto.ETHEREUM, ethereumNftItemApi)
    }

    @Bean
    fun ethereumOwnershipService(@Qualifier("ethereum.ownership.api") ethereumNftOwnershipApi: NftOwnershipControllerApi): OwnershipService {
        return EthereumOwnershipService(EthBlockchainDto.ETHEREUM, ethereumNftOwnershipApi)
    }

    @Bean
    fun ethereumCollectionService(@Qualifier("ethereum.collection.api") ethereumNftCollectionApi: NftCollectionControllerApi): CollectionService {
        return EthereumCollectionService(EthBlockchainDto.ETHEREUM, ethereumNftCollectionApi)
    }

    //--------------------- POLYGON ---------------------//
    @Bean
    fun polygonItemService(@Qualifier("polygon.item.api") ethereumNftItemApi: NftItemControllerApi): ItemService {
        return EthereumItemService(EthBlockchainDto.POLYGON, ethereumNftItemApi)
    }

    @Bean
    fun polygonOwnershipService(@Qualifier("polygon.ownership.api") ethereumNftOwnershipApi: NftOwnershipControllerApi): OwnershipService {
        return EthereumOwnershipService(EthBlockchainDto.POLYGON, ethereumNftOwnershipApi)
    }

    @Bean
    fun polygonCollectionService(@Qualifier("polygon.collection.api") ethereumNftCollectionApi: NftCollectionControllerApi): CollectionService {
        return EthereumCollectionService(EthBlockchainDto.POLYGON, ethereumNftCollectionApi)
    }

    //---------------------- FLOW -----------------------//
    @Bean
    fun flowItemService(flowNftItemApi: FlowNftItemControllerApi): ItemService {
        return FlowItemService(FlowBlockchainDto.FLOW, flowNftItemApi)
    }

    @Bean
    fun flowOwnershipService(flowNftOwnershipApi: FlowNftOwnershipControllerApi): OwnershipService {
        return FlowOwnershipService(FlowBlockchainDto.FLOW, flowNftOwnershipApi)
    }

    @Bean
    fun flowCollectionService(flowNftCollectionApi: FlowNftCollectionControllerApi): CollectionService {
        return FlowCollectionService(FlowBlockchainDto.FLOW, flowNftCollectionApi)
    }

}