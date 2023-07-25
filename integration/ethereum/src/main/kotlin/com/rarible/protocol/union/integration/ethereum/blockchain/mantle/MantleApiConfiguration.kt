package com.rarible.protocol.union.integration.ethereum.blockchain.mantle

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
import com.rarible.protocol.union.integration.ethereum.EthApiFactoryConfiguration
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
import com.rarible.protocol.union.integration.ethereum.service.MantleActivityService
import com.rarible.protocol.union.integration.ethereum.service.MantleAuctionService
import com.rarible.protocol.union.integration.ethereum.service.MantleCollectionService
import com.rarible.protocol.union.integration.ethereum.service.MantleDomainService
import com.rarible.protocol.union.integration.ethereum.service.MantleItemService
import com.rarible.protocol.union.integration.ethereum.service.MantleOrderService
import com.rarible.protocol.union.integration.ethereum.service.MantleOwnershipService
import com.rarible.protocol.union.integration.ethereum.service.MantleSignatureService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@MantleConfiguration
@Import(EthApiFactoryConfiguration::class)
@EnableConfigurationProperties(value = [MantleIntegrationProperties::class])
class MantleApiConfiguration {

    private val mantle = BlockchainDto.MANTLE.name.lowercase()

    @Bean
    fun mantleBlockchain(): BlockchainDto {
        return BlockchainDto.MANTLE
    }

    //-------------------- API --------------------//

    @Bean
    @Qualifier("mantle.item.api")
    fun mantleItemApi(factory: NftIndexerApiClientFactory): NftItemControllerApi =
        factory.createNftItemApiClient(mantle)

    @Bean
    @Qualifier("mantle.ownership.api")
    fun mantleOwnershipApi(factory: NftIndexerApiClientFactory): NftOwnershipControllerApi =
        factory.createNftOwnershipApiClient(mantle)

    @Bean
    @Qualifier("mantle.collection.api")
    fun mantleCollectionApi(factory: NftIndexerApiClientFactory): NftCollectionControllerApi =
        factory.createNftCollectionApiClient(mantle)

    @Bean
    @Qualifier("mantle.order.api")
    fun mantleOrderApi(factory: OrderIndexerApiClientFactory): OrderControllerApi =
        factory.createOrderApiClient(mantle)

    @Bean
    @Qualifier("mantle.auction.api")
    fun mantleAuctionApi(factory: OrderIndexerApiClientFactory): AuctionControllerApi =
        factory.createAuctionApiClient(mantle)

    @Bean
    @Qualifier("mantle.signature.api")
    fun mantleSignatureApi(factory: OrderIndexerApiClientFactory): OrderSignatureControllerApi =
        factory.createOrderSignatureApiClient(mantle)

    @Bean
    @Qualifier("mantle.admin.api.order")
    fun mantleOrderAdminApi(factory: OrderIndexerApiClientFactory): OrderAdminControllerApi =
        factory.createOrderAdminApiClient(mantle)

    @Bean
    @Qualifier("mantle.domain.api")
    fun mantleDomainApi(factory: NftIndexerApiClientFactory): NftDomainControllerApi =
        factory.createNftDomainApiClient(mantle)

    @Bean
    @Qualifier("mantle.activity.api.item")
    fun mantleActivityItemApi(factory: NftIndexerApiClientFactory): NftActivityControllerApi =
        factory.createNftActivityApiClient(mantle)

    @Bean
    @Qualifier("mantle.activity.api.order")
    fun mantleActivityOrderApi(factory: OrderIndexerApiClientFactory): OrderActivityControllerApi =
        factory.createOrderActivityApiClient(mantle)

    @Bean
    @Qualifier("mantle.activity.api.auction")
    fun mantleActivityAuctionApi(factory: OrderIndexerApiClientFactory): AuctionActivityControllerApi =
        factory.createAuctionActivityApiClient(mantle)

    //-------------------- Services --------------------//

    @Bean
    fun mantleItemService(
        @Qualifier("mantle.item.api") controllerApi: NftItemControllerApi
    ): EthItemService {
        return MantleItemService(controllerApi)
    }

    @Bean
    fun mantleOwnershipService(
        @Qualifier("mantle.ownership.api") controllerApi: NftOwnershipControllerApi
    ): EthOwnershipService {
        return MantleOwnershipService(controllerApi)
    }

    @Bean
    fun mantleCollectionService(
        @Qualifier("mantle.collection.api") controllerApi: NftCollectionControllerApi
    ): EthCollectionService {
        return MantleCollectionService(controllerApi)
    }

    @Bean
    fun mantleOrderService(
        @Qualifier("mantle.order.api") controllerApi: OrderControllerApi,
        @Qualifier("mantle.admin.api.order") adminControllerApi: OrderAdminControllerApi,
        converter: EthOrderConverter
    ): EthOrderService {
        return MantleOrderService(controllerApi, adminControllerApi, converter)
    }

    @Bean
    fun mantleAuctionService(
        @Qualifier("mantle.auction.api") auctionApi: AuctionControllerApi,
        converter: EthAuctionConverter
    ): EthAuctionService {
        return MantleAuctionService(auctionApi, converter)
    }

    @Bean
    fun mantleSignatureService(
        @Qualifier("mantle.signature.api") controllerApi: OrderSignatureControllerApi
    ): EthSignatureService {
        return MantleSignatureService(controllerApi)
    }

    @Bean
    fun mantleDomainService(
        @Qualifier("mantle.domain.api") controllerApi: NftDomainControllerApi,
    ): EthDomainService {
        return MantleDomainService(controllerApi)
    }

    @Bean
    fun mantleActivityService(
        @Qualifier("mantle.activity.api.item") itemActivityApi: NftActivityControllerApi,
        @Qualifier("mantle.activity.api.order") orderActivityApi: OrderActivityControllerApi,
        @Qualifier("mantle.activity.api.auction") auctionActivityApi: AuctionActivityControllerApi,
        converter: EthActivityConverter
    ): EthActivityService {
        return MantleActivityService(itemActivityApi, orderActivityApi, auctionActivityApi, converter)
    }
}
