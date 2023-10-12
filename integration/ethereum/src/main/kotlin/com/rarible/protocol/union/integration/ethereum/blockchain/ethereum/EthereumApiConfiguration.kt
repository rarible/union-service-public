package com.rarible.protocol.union.integration.ethereum.blockchain.ethereum

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

@EthereumConfiguration
@Import(EthApiFactoryConfiguration::class)
@EnableConfigurationProperties(value = [EthereumIntegrationProperties::class])
class EthereumApiConfiguration {

    private val blockchain = BlockchainDto.ETHEREUM
    private val blockchainName = blockchain.name.lowercase()

    @Bean
    fun ethereumBlockchain(): BlockchainDto {
        return blockchain
    }

    // -------------------- API --------------------//

    @Bean
    @Qualifier("ethereum.balance.api")
    fun ethereumBalanceApi(factory: Erc20IndexerApiClientFactory): BalanceControllerApi =
        factory.createBalanceApiClient(blockchainName)

    @Bean
    @Qualifier("ethereum.item.api")
    fun ethereumItemApi(factory: NftIndexerApiClientFactory): NftItemControllerApi =
        factory.createNftItemApiClient(blockchainName)

    @Bean
    @Qualifier("ethereum.item.lazy.api")
    fun ethereumLazyItemApi(factory: NftIndexerApiClientFactory): NftLazyMintControllerApi =
        factory.createNftMintApiClient(blockchainName)

    @Bean
    @Qualifier("ethereum.ownership.api")
    fun ethereumOwnershipApi(factory: NftIndexerApiClientFactory): NftOwnershipControllerApi =
        factory.createNftOwnershipApiClient(blockchainName)

    @Bean
    @Qualifier("ethereum.collection.api")
    fun ethereumCollectionApi(factory: NftIndexerApiClientFactory): NftCollectionControllerApi =
        factory.createNftCollectionApiClient(blockchainName)

    @Bean
    @Qualifier("ethereum.order.api")
    fun ethereumOrderApi(factory: OrderIndexerApiClientFactory): OrderControllerApi =
        factory.createOrderApiClient(blockchainName)

    @Bean
    @Qualifier("ethereum.auction.api")
    fun ethereumAuctionApi(factory: OrderIndexerApiClientFactory): AuctionControllerApi =
        factory.createAuctionApiClient(blockchainName)

    @Bean
    @Qualifier("ethereum.signature.api")
    fun ethereumSignatureApi(factory: OrderIndexerApiClientFactory): OrderSignatureControllerApi =
        factory.createOrderSignatureApiClient(blockchainName)

    @Bean
    @Qualifier("ethereum.admin.api.order")
    fun ethereumOrderAdminApi(factory: OrderIndexerApiClientFactory): OrderAdminControllerApi =
        factory.createOrderAdminApiClient(blockchainName)

    @Bean
    @Qualifier("ethereum.domain.api")
    fun ethereumDomainApi(factory: NftIndexerApiClientFactory): NftDomainControllerApi =
        factory.createNftDomainApiClient(blockchainName)

    @Bean
    @Qualifier("ethereum.activity.api.item")
    fun ethereumActivityItemApi(factory: NftIndexerApiClientFactory): NftActivityControllerApi =
        factory.createNftActivityApiClient(blockchainName)

    @Bean
    @Qualifier("ethereum.activity.api.order")
    fun ethereumActivityOrderApi(factory: OrderIndexerApiClientFactory): OrderActivityControllerApi =
        factory.createOrderActivityApiClient(blockchainName)

    @Bean
    @Qualifier("ethereum.activity.api.auction")
    fun ethereumActivityAuctionApi(factory: OrderIndexerApiClientFactory): AuctionActivityControllerApi =
        factory.createAuctionActivityApiClient(blockchainName)

    // -------------------- Services --------------------//

    @Bean
    fun ethereumBalanceService(
        @Qualifier("ethereum.balance.api") controllerApi: BalanceControllerApi
    ): EthBalanceService {
        return EthBalanceService(blockchain, controllerApi)
    }

    @Bean
    fun ethereumItemService(
        @Qualifier("ethereum.item.api") controllerApi: NftItemControllerApi,
        @Qualifier("ethereum.item.lazy.api") lazyMintControllerApi: NftLazyMintControllerApi
    ): EthItemService {
        return EthItemService(blockchain, controllerApi, lazyMintControllerApi)
    }

    @Bean
    fun ethereumOwnershipService(
        @Qualifier("ethereum.ownership.api") controllerApi: NftOwnershipControllerApi
    ): EthOwnershipService {
        return EthOwnershipService(blockchain, controllerApi)
    }

    @Bean
    fun ethereumCollectionService(
        @Qualifier("ethereum.collection.api") controllerApi: NftCollectionControllerApi
    ): EthCollectionService {
        return EthCollectionService(blockchain, controllerApi)
    }

    @Bean
    fun ethereumOrderService(
        @Qualifier("ethereum.order.api") controllerApi: OrderControllerApi,
        @Qualifier("ethereum.admin.api.order") adminControllerApi: OrderAdminControllerApi,
        converter: EthOrderConverter
    ): EthOrderService {
        return EthOrderService(blockchain, controllerApi, adminControllerApi, converter)
    }

    @Bean
    fun ethereumAuctionService(
        @Qualifier("ethereum.auction.api") auctionApi: AuctionControllerApi,
        converter: EthAuctionConverter
    ): EthAuctionService {
        return EthAuctionService(blockchain, auctionApi, converter)
    }

    @Bean
    fun ethereumSignatureService(
        @Qualifier("ethereum.signature.api") controllerApi: OrderSignatureControllerApi
    ): EthSignatureService {
        return EthSignatureService(blockchain, controllerApi)
    }

    @Bean
    fun ethereumDomainService(
        @Qualifier("ethereum.domain.api") controllerApi: NftDomainControllerApi,
    ): EthDomainService {
        return EthDomainService(blockchain, controllerApi)
    }

    @Bean
    fun ethereumActivityService(
        @Qualifier("ethereum.activity.api.item") itemActivityApi: NftActivityControllerApi,
        @Qualifier("ethereum.activity.api.order") orderActivityApi: OrderActivityControllerApi,
        @Qualifier("ethereum.activity.api.auction") auctionActivityApi: AuctionActivityControllerApi,
        converter: EthActivityConverter
    ): EthActivityService {
        return EthActivityService(blockchain, itemActivityApi, orderActivityApi, auctionActivityApi, converter)
    }
}
