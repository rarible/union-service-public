package com.rarible.protocol.union.integration.ethereum

import com.rarible.protocol.nft.api.client.NftActivityControllerApi
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.nft.api.client.NftDomainControllerApi
import com.rarible.protocol.nft.api.client.NftIndexerApiClientFactory
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.order.api.client.AuctionActivityControllerApi
import com.rarible.protocol.order.api.client.AuctionControllerApi
import com.rarible.protocol.order.api.client.OrderActivityControllerApi
import com.rarible.protocol.order.api.client.OrderAdminControllerApi
import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.order.api.client.OrderIndexerApiClientFactory
import com.rarible.protocol.order.api.client.OrderSignatureControllerApi
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthActivityConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthAuctionConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.service.EthActivityService
import com.rarible.protocol.union.integration.ethereum.service.EthAuctionService
import com.rarible.protocol.union.integration.ethereum.service.EthCollectionService
import com.rarible.protocol.union.integration.ethereum.service.EthDomainService
import com.rarible.protocol.union.integration.ethereum.service.EthItemService
import com.rarible.protocol.union.integration.ethereum.service.EthOrderService
import com.rarible.protocol.union.integration.ethereum.service.EthOwnershipService
import com.rarible.protocol.union.integration.ethereum.service.EthSignatureService
import com.rarible.protocol.union.integration.ethereum.service.PolygonActivityService
import com.rarible.protocol.union.integration.ethereum.service.PolygonAuctionService
import com.rarible.protocol.union.integration.ethereum.service.PolygonCollectionService
import com.rarible.protocol.union.integration.ethereum.service.PolygonDomainService
import com.rarible.protocol.union.integration.ethereum.service.PolygonItemService
import com.rarible.protocol.union.integration.ethereum.service.PolygonOrderService
import com.rarible.protocol.union.integration.ethereum.service.PolygonOwnershipService
import com.rarible.protocol.union.integration.ethereum.service.PolygonSignatureService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@PolygonConfiguration
@Import(EthApiFactoryConfiguration::class)
@EnableConfigurationProperties(value = [PolygonIntegrationProperties::class])
class PolygonApiConfiguration {

    private val polygon = BlockchainDto.POLYGON.name.lowercase()

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
    @Qualifier("polygon.auction.api")
    fun polygonAuctionApi(factory: OrderIndexerApiClientFactory): AuctionControllerApi =
        factory.createAuctionApiClient(polygon)

    @Bean
    @Qualifier("polygon.signature.api")
    fun polygonSignatureApi(factory: OrderIndexerApiClientFactory): OrderSignatureControllerApi =
        factory.createOrderSignatureApiClient(polygon)

    @Bean
    @Qualifier("polygon.admin.api.order")
    fun ethereumOrderAdminApi(factory: OrderIndexerApiClientFactory): OrderAdminControllerApi =
        factory.createOrderAdminApiClient(polygon)

    @Bean
    @Qualifier("polygon.domain.api")
    fun polygonDomainApi(factory: NftIndexerApiClientFactory): NftDomainControllerApi =
        factory.createNftDomainApiClient(polygon)

    @Bean
    @Qualifier("polygon.activity.api.item")
    fun polygonActivityItemApi(factory: NftIndexerApiClientFactory): NftActivityControllerApi =
        factory.createNftActivityApiClient(polygon)

    @Bean
    @Qualifier("polygon.activity.api.order")
    fun polygonActivityOrderApi(factory: OrderIndexerApiClientFactory): OrderActivityControllerApi =
        factory.createOrderActivityApiClient(polygon)

    @Bean
    @Qualifier("polygon.activity.api.auction")
    fun polygonActivityAuctionApi(factory: OrderIndexerApiClientFactory): AuctionActivityControllerApi =
        factory.createAuctionActivityApiClient(polygon)

    //-------------------- Services --------------------//

    @Bean
    fun polygonItemService(
        @Qualifier("polygon.item.api") controllerApi: NftItemControllerApi
    ): EthItemService {
        return PolygonItemService(controllerApi)
    }

    @Bean
    fun polygonOwnershipService(
        @Qualifier("polygon.ownership.api") controllerApi: NftOwnershipControllerApi
    ): EthOwnershipService {
        return PolygonOwnershipService(controllerApi)
    }

    @Bean
    fun polygonCollectionService(
        @Qualifier("polygon.collection.api") controllerApi: NftCollectionControllerApi
    ): EthCollectionService {
        return PolygonCollectionService(controllerApi)
    }

    @Bean
    fun polygonOrderService(
        @Qualifier("polygon.order.api") controllerApi: OrderControllerApi,
        @Qualifier("polygon.admin.api.order") adminControllerApi: OrderAdminControllerApi,
        converter: EthOrderConverter
    ): EthOrderService {
        return PolygonOrderService(controllerApi, adminControllerApi, converter)
    }

    @Bean
    fun polygonAuctionService(
        @Qualifier("polygon.auction.api") auctionApi: AuctionControllerApi,
        converter: EthAuctionConverter
    ): EthAuctionService {
        return PolygonAuctionService(auctionApi, converter)
    }

    @Bean
    fun polygonSignatureService(
        @Qualifier("polygon.signature.api") controllerApi: OrderSignatureControllerApi
    ): EthSignatureService {
        return PolygonSignatureService(controllerApi)
    }

    @Bean
    fun polygonDomainService(
        @Qualifier("polygon.domain.api") controllerApi: NftDomainControllerApi,
    ): EthDomainService {
        return PolygonDomainService(controllerApi)
    }

    @Bean
    fun polygonActivityService(
        @Qualifier("polygon.activity.api.item") itemActivityApi: NftActivityControllerApi,
        @Qualifier("polygon.activity.api.order") orderActivityApi: OrderActivityControllerApi,
        @Qualifier("polygon.activity.api.auction") auctionActivityApi: AuctionActivityControllerApi,
        converter: EthActivityConverter
    ): EthActivityService {
        return PolygonActivityService(itemActivityApi, orderActivityApi, auctionActivityApi, converter)
    }
}
