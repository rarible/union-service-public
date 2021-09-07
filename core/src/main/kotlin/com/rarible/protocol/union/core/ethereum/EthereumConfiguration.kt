package com.rarible.protocol.union.core.ethereum

import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.nft.api.client.NftIndexerApiClientFactory
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.order.api.client.OrderIndexerApiClientFactory
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EthereumConfiguration {

    private val ethereum = BlockchainDto.ETHEREUM.name.toLowerCase()
    private val polygon = BlockchainDto.POLYGON.name.toLowerCase()

    //--------------------- ETHEREUM ---------------------//
    @Bean
    @Qualifier("ethereum.item.api")
    fun ethereumItemApi(factory: NftIndexerApiClientFactory): NftItemControllerApi =
        factory.createNftItemApiClient(ethereum)

    @Bean
    @Qualifier("ethereum.ownership.api")
    fun ethereumOwnershipApi(factory: NftIndexerApiClientFactory): NftOwnershipControllerApi =
        factory.createNftOwnershipApiClient(ethereum)

    @Bean
    @Qualifier("ethereum.collection.api")
    fun ethereumCollectionApi(factory: NftIndexerApiClientFactory): NftCollectionControllerApi =
        factory.createNftCollectionApiClient(ethereum)

    @Bean
    @Qualifier("ethereum.order.api")
    fun ethereumOrderApi(factory: OrderIndexerApiClientFactory): OrderControllerApi =
        factory.createOrderApiClient(ethereum)

    // TODO not sure that's good idea to use nft-order client
    /*
    @Bean
    @Qualifier("ethereum.activity.api")
    fun ethereumOrderActivityApi(factory: NftOrderApiClientFactory): NftOrderActivityControllerApi =
        factory.createNftOrderActivityApiClient(ethereum)
    */

    //--------------------- POLYGON ---------------------//
    @Bean
    @Qualifier("polygon.item.api")
    fun polygonItemApi(factory: NftIndexerApiClientFactory): NftItemControllerApi =
        factory.createNftItemApiClient(polygon)

    @Bean
    @Qualifier("polygon.ownership.api")
    fun polygonOwnershipApi(factory: NftIndexerApiClientFactory): NftOwnershipControllerApi =
        factory.createNftOwnershipApiClient(polygon)

    @Bean
    @Qualifier("polygon.collection.api")
    fun polygonCollectionApi(factory: NftIndexerApiClientFactory): NftCollectionControllerApi =
        factory.createNftCollectionApiClient(polygon)

    @Bean
    @Qualifier("polygon.order.api")
    fun polygonOrderApi(factory: OrderIndexerApiClientFactory): OrderControllerApi =
        factory.createOrderApiClient(polygon)

    // TODO not sure that's good idea to use nft-order client
    /*
    @Bean
    @Qualifier("polygon.activity.api")
    fun polygonOrderActivityApi(factory: NftOrderApiClientFactory): NftOrderActivityControllerApi =
        factory.createNftOrderActivityApiClient(polygon)
    */

}