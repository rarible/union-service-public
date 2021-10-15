package com.rarible.protocol.union.core.ethereum

import com.rarible.protocol.nft.api.client.NftActivityControllerApi
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.nft.api.client.NftIndexerApiClientFactory
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.order.api.client.OrderActivityControllerApi
import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.order.api.client.OrderIndexerApiClientFactory
import com.rarible.protocol.order.api.client.OrderSignatureControllerApi
import com.rarible.protocol.union.core.ethereum.converter.EthActivityConverter
import com.rarible.protocol.union.core.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.core.ethereum.service.EthereumActivityService
import com.rarible.protocol.union.core.ethereum.service.EthereumCollectionService
import com.rarible.protocol.union.core.ethereum.service.EthereumItemService
import com.rarible.protocol.union.core.ethereum.service.EthereumOrderService
import com.rarible.protocol.union.core.ethereum.service.EthereumOwnershipService
import com.rarible.protocol.union.core.ethereum.service.EthereumSignatureService
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EthereumConfiguration {

    private val ethereum = BlockchainDto.ETHEREUM.name.toLowerCase()
    private val polygon = BlockchainDto.POLYGON.name.toLowerCase()

    //--------------------- ETHEREUM API ---------------------//
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

    //--------------------- POLYGON API ---------------------//
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

    @Bean
    @Qualifier("polygon.signature.api")
    fun polygonSignatureApi(factory: OrderIndexerApiClientFactory): OrderSignatureControllerApi =
        factory.createOrderSignatureApiClient(polygon)

    @Bean
    @Qualifier("polygon.activity.api.item")
    fun polygonActivityItemApi(factory: NftIndexerApiClientFactory): NftActivityControllerApi =
        factory.createNftActivityApiClient(polygon)

    @Bean
    @Qualifier("polygon.activity.api.order")
    fun polygonActivityOrderApi(factory: OrderIndexerApiClientFactory): OrderActivityControllerApi =
        factory.createOrderActivityApiClient(polygon)

    //--------------------- ETHEREUM SERVICES --------------------//
    @Bean
    fun ethereumItemService(@Qualifier("ethereum.item.api") ethereumItemApi: NftItemControllerApi): ItemService {
        return EthereumItemService(BlockchainDto.ETHEREUM, ethereumItemApi)
    }

    @Bean
    fun ethereumOwnershipService(@Qualifier("ethereum.ownership.api") ethereumOwnershipApi: NftOwnershipControllerApi): OwnershipService {
        return EthereumOwnershipService(BlockchainDto.ETHEREUM, ethereumOwnershipApi)
    }

    @Bean
    fun ethereumCollectionService(@Qualifier("ethereum.collection.api") ethereumCollectionApi: NftCollectionControllerApi): CollectionService {
        return EthereumCollectionService(BlockchainDto.ETHEREUM, ethereumCollectionApi)
    }

    @Bean
    fun ethereumOrderService(
        @Qualifier("ethereum.order.api") ethereumOrderApi: OrderControllerApi,
        ethOrderConverter: EthOrderConverter
    ): OrderService {
        return EthereumOrderService(BlockchainDto.ETHEREUM, ethereumOrderApi, ethOrderConverter)
    }

    @Bean
    fun ethereumSignatureService(@Qualifier("ethereum.signature.api") ethereumSignatureApi: OrderSignatureControllerApi): SignatureService {
        return EthereumSignatureService(BlockchainDto.ETHEREUM, ethereumSignatureApi)
    }

    @Bean
    fun ethereumActivityService(
        @Qualifier("ethereum.activity.api.item") ethereumActivityItemApi: NftActivityControllerApi,
        @Qualifier("ethereum.activity.api.order") ethereumActivityOrderApi: OrderActivityControllerApi,
        ethActivityConverter: EthActivityConverter
    ): ActivityService {
        return EthereumActivityService(BlockchainDto.ETHEREUM, ethereumActivityItemApi, ethereumActivityOrderApi, ethActivityConverter)
    }

    //--------------------- POLYGON SERVICES ---------------------//
    @Bean
    fun polygonItemService(@Qualifier("polygon.item.api") ethereumItemApi: NftItemControllerApi): ItemService {
        return EthereumItemService(BlockchainDto.POLYGON, ethereumItemApi)
    }

    @Bean
    fun polygonOwnershipService(@Qualifier("polygon.ownership.api") ethereumOwnershipApi: NftOwnershipControllerApi): OwnershipService {
        return EthereumOwnershipService(BlockchainDto.POLYGON, ethereumOwnershipApi)
    }

    @Bean
    fun polygonCollectionService(@Qualifier("polygon.collection.api") ethereumCollectionApi: NftCollectionControllerApi): CollectionService {
        return EthereumCollectionService(BlockchainDto.POLYGON, ethereumCollectionApi)
    }

    @Bean
    fun polygonOrderService(
        @Qualifier("polygon.order.api") ethereumOrderApi: OrderControllerApi,
        ethOrderConverter: EthOrderConverter
    ): OrderService {
        return EthereumOrderService(BlockchainDto.POLYGON, ethereumOrderApi, ethOrderConverter)
    }

    @Bean
    fun polygonSignatureService(@Qualifier("polygon.signature.api") ethereumSignatureApi: OrderSignatureControllerApi): SignatureService {
        return EthereumSignatureService(BlockchainDto.POLYGON, ethereumSignatureApi)
    }

    @Bean
    fun polygonActivityService(
        @Qualifier("polygon.activity.api.item") polygonActivityItemApi: NftActivityControllerApi,
        @Qualifier("polygon.activity.api.order") polygonActivityOrderApi: OrderActivityControllerApi,
        ethActivityConverter: EthActivityConverter
    ): ActivityService {
        return EthereumActivityService(BlockchainDto.POLYGON, polygonActivityItemApi, polygonActivityOrderApi, ethActivityConverter)
    }
}