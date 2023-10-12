package com.rarible.protocol.union.integration.ethereum.blockchain.polygon

import com.rarible.protocol.erc20.api.client.BalanceControllerApi
import com.rarible.protocol.erc20.api.client.Erc20IndexerApiClientFactory
import com.rarible.protocol.nft.api.client.NftActivityControllerApi
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.nft.api.client.NftDomainControllerApi
import com.rarible.protocol.nft.api.client.NftIndexerApiClientFactory
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftLazyMintControllerApi
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
import com.rarible.protocol.union.integration.ethereum.service.EthBalanceService
import com.rarible.protocol.union.integration.ethereum.service.EthCollectionService
import com.rarible.protocol.union.integration.ethereum.service.EthDomainService
import com.rarible.protocol.union.integration.ethereum.service.EthItemService
import com.rarible.protocol.union.integration.ethereum.service.EthOrderService
import com.rarible.protocol.union.integration.ethereum.service.EthOwnershipService
import com.rarible.protocol.union.integration.ethereum.service.EthSignatureService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@PolygonConfiguration
@Import(EthApiFactoryConfiguration::class)
@EnableConfigurationProperties(value = [PolygonIntegrationProperties::class])
class PolygonApiConfiguration {

    private val blockchain = BlockchainDto.POLYGON
    private val blockchainName = blockchain.name.lowercase()

    @Bean
    fun polygonBlockchain(): BlockchainDto {
        return blockchain
    }

    // -------------------- API --------------------//

    @Bean
    @Qualifier("polygon.balance.api")
    fun polygonBalanceApi(factory: Erc20IndexerApiClientFactory): BalanceControllerApi =
        factory.createBalanceApiClient(blockchainName)

    @Bean
    @Qualifier("polygon.item.api")
    fun polygonItemApi(factory: NftIndexerApiClientFactory): NftItemControllerApi =
        factory.createNftItemApiClient(blockchainName)

    @Bean
    @Qualifier("polygon.item.lazy.api")
    fun polygonLazyItemApi(factory: NftIndexerApiClientFactory): NftLazyMintControllerApi =
        factory.createNftMintApiClient(blockchainName)

    @Bean
    @Qualifier("polygon.ownership.api")
    fun polygonOwnershipApi(factory: NftIndexerApiClientFactory): NftOwnershipControllerApi =
        factory.createNftOwnershipApiClient(blockchainName)

    @Bean
    @Qualifier("polygon.collection.api")
    fun polygonCollectionApi(factory: NftIndexerApiClientFactory): NftCollectionControllerApi =
        factory.createNftCollectionApiClient(blockchainName)

    @Bean
    @Qualifier("polygon.order.api")
    fun polygonOrderApi(factory: OrderIndexerApiClientFactory): OrderControllerApi =
        factory.createOrderApiClient(blockchainName)

    @Bean
    @Qualifier("polygon.auction.api")
    fun polygonAuctionApi(factory: OrderIndexerApiClientFactory): AuctionControllerApi =
        factory.createAuctionApiClient(blockchainName)

    @Bean
    @Qualifier("polygon.signature.api")
    fun polygonSignatureApi(factory: OrderIndexerApiClientFactory): OrderSignatureControllerApi =
        factory.createOrderSignatureApiClient(blockchainName)

    @Bean
    @Qualifier("polygon.admin.api.order")
    fun polygonOrderAdminApi(factory: OrderIndexerApiClientFactory): OrderAdminControllerApi =
        factory.createOrderAdminApiClient(blockchainName)

    @Bean
    @Qualifier("polygon.domain.api")
    fun polygonDomainApi(factory: NftIndexerApiClientFactory): NftDomainControllerApi =
        factory.createNftDomainApiClient(blockchainName)

    @Bean
    @Qualifier("polygon.activity.api.item")
    fun polygonActivityItemApi(factory: NftIndexerApiClientFactory): NftActivityControllerApi =
        factory.createNftActivityApiClient(blockchainName)

    @Bean
    @Qualifier("polygon.activity.api.order")
    fun polygonActivityOrderApi(factory: OrderIndexerApiClientFactory): OrderActivityControllerApi =
        factory.createOrderActivityApiClient(blockchainName)

    @Bean
    @Qualifier("polygon.activity.api.auction")
    fun polygonActivityAuctionApi(factory: OrderIndexerApiClientFactory): AuctionActivityControllerApi =
        factory.createAuctionActivityApiClient(blockchainName)

    // -------------------- Services --------------------//

    @Bean
    fun polygonBalanceService(
        @Qualifier("polygon.balance.api") controllerApi: BalanceControllerApi
    ): EthBalanceService {
        return EthBalanceService(blockchain, controllerApi)
    }

    @Bean
    fun polygonItemService(
        @Qualifier("polygon.item.api") controllerApi: NftItemControllerApi,
        @Qualifier("polygon.item.lazy.api") lazyMintControllerApi: NftLazyMintControllerApi
    ): EthItemService {
        return EthItemService(blockchain, controllerApi, lazyMintControllerApi)
    }

    @Bean
    fun polygonOwnershipService(
        @Qualifier("polygon.ownership.api") controllerApi: NftOwnershipControllerApi
    ): EthOwnershipService {
        return EthOwnershipService(blockchain, controllerApi)
    }

    @Bean
    fun polygonCollectionService(
        @Qualifier("polygon.collection.api") controllerApi: NftCollectionControllerApi
    ): EthCollectionService {
        return EthCollectionService(blockchain, controllerApi)
    }

    @Bean
    fun polygonOrderService(
        @Qualifier("polygon.order.api") controllerApi: OrderControllerApi,
        @Qualifier("polygon.admin.api.order") adminControllerApi: OrderAdminControllerApi,
        converter: EthOrderConverter
    ): EthOrderService {
        return EthOrderService(blockchain, controllerApi, adminControllerApi, converter)
    }

    @Bean
    fun polygonAuctionService(
        @Qualifier("polygon.auction.api") auctionApi: AuctionControllerApi,
        converter: EthAuctionConverter
    ): EthAuctionService {
        return EthAuctionService(blockchain, auctionApi, converter)
    }

    @Bean
    fun polygonSignatureService(
        @Qualifier("polygon.signature.api") controllerApi: OrderSignatureControllerApi
    ): EthSignatureService {
        return EthSignatureService(blockchain, controllerApi)
    }

    @Bean
    fun polygonDomainService(
        @Qualifier("polygon.domain.api") controllerApi: NftDomainControllerApi,
    ): EthDomainService {
        return EthDomainService(blockchain, controllerApi)
    }

    @Bean
    fun polygonActivityService(
        @Qualifier("polygon.activity.api.item") itemActivityApi: NftActivityControllerApi,
        @Qualifier("polygon.activity.api.order") orderActivityApi: OrderActivityControllerApi,
        @Qualifier("polygon.activity.api.auction") auctionActivityApi: AuctionActivityControllerApi,
        converter: EthActivityConverter
    ): EthActivityService {
        return EthActivityService(blockchain, itemActivityApi, orderActivityApi, auctionActivityApi, converter)
    }
}
