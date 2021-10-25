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
import com.rarible.protocol.union.integration.ethereum.converter.EthActivityConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.service.EthActivityService
import com.rarible.protocol.union.integration.ethereum.service.EthCollectionService
import com.rarible.protocol.union.integration.ethereum.service.EthItemService
import com.rarible.protocol.union.integration.ethereum.service.EthOrderService
import com.rarible.protocol.union.integration.ethereum.service.EthOwnershipService
import com.rarible.protocol.union.integration.ethereum.service.EthSignatureService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import

@PolygonConfiguration
@Import(CoreConfiguration::class)
@ComponentScan(basePackageClasses = [EthOrderConverter::class])
@EnableConfigurationProperties(value = [PolygonIntegrationProperties::class])
class PolygonApiConfiguration {

    private val polygon = BlockchainDto.POLYGON.name.toLowerCase()

    @Bean
    fun polygonBlockchain(): BlockchainDto {
        return BlockchainDto.POLYGON
    }

    //-------------------- API --------------------//

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

    //-------------------- Services --------------------//

    @Bean
    fun polygonItemService(
        @Qualifier("polygon.item.api") controllerApi: NftItemControllerApi
    ): EthItemService {
        return EthItemService(BlockchainDto.POLYGON, controllerApi)
    }

    @Bean
    fun polygonOwnershipService(
        @Qualifier("polygon.ownership.api") controllerApi: NftOwnershipControllerApi
    ): EthOwnershipService {
        return EthOwnershipService(BlockchainDto.POLYGON, controllerApi)
    }

    @Bean
    fun polygonCollectionService(
        @Qualifier("polygon.collection.api") controllerApi: NftCollectionControllerApi
    ): EthCollectionService {
        return EthCollectionService(BlockchainDto.POLYGON, controllerApi)
    }

    @Bean
    fun polygonOrderService(
        @Qualifier("polygon.order.api") controllerApi: OrderControllerApi,
        converter: EthOrderConverter
    ): EthOrderService {
        return EthOrderService(BlockchainDto.POLYGON, controllerApi, converter)
    }

    @Bean
    fun polygonSignatureService(
        @Qualifier("polygon.signature.api") controllerApi: OrderSignatureControllerApi
    ): EthSignatureService {
        return EthSignatureService(BlockchainDto.POLYGON, controllerApi)
    }

    @Bean
    fun polygonActivityService(
        @Qualifier("polygon.activity.api.item") itemActivityApi: NftActivityControllerApi,
        @Qualifier("polygon.activity.api.order") orderActivityApi: OrderActivityControllerApi,
        converter: EthActivityConverter
    ): EthActivityService {
        return EthActivityService(BlockchainDto.POLYGON, itemActivityApi, orderActivityApi, converter)
    }
}