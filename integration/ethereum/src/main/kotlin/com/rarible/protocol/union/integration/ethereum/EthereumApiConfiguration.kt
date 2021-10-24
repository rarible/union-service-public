package com.rarible.protocol.union.integration.ethereum

import com.rarible.protocol.nft.api.client.NftActivityControllerApi
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.nft.api.client.NftIndexerApiClientFactory
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.order.api.client.OrderActivityControllerApi
import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.order.api.client.OrderIndexerApiClientFactory
import com.rarible.protocol.order.api.client.OrderSignatureControllerApi
import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(CoreConfiguration::class)
@ComponentScan(basePackageClasses = [EthereumApiConfiguration::class])
@EnableConfigurationProperties(value = [EthereumIntegrationProperties::class])
class EthereumApiConfiguration {

    private val ethereum = BlockchainDto.ETHEREUM.name.toLowerCase()

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

    @Bean
    @Qualifier("ethereum.signature.api")
    fun ethereumSignatureApi(factory: OrderIndexerApiClientFactory): OrderSignatureControllerApi =
        factory.createOrderSignatureApiClient(ethereum)

    @Bean
    @Qualifier("ethereum.activity.api.item")
    fun ethereumActivityItemApi(factory: NftIndexerApiClientFactory): NftActivityControllerApi =
        factory.createNftActivityApiClient(ethereum)

    @Bean
    @Qualifier("ethereum.activity.api.order")
    fun ethereumActivityOrderApi(factory: OrderIndexerApiClientFactory): OrderActivityControllerApi =
        factory.createOrderActivityApiClient(ethereum)
}