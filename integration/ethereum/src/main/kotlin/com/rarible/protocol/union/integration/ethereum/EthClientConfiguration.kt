package com.rarible.protocol.union.integration.ethereum

import com.rarible.protocol.erc20.api.client.BalanceControllerApi
import com.rarible.protocol.erc20.api.client.Erc20IndexerApiClientFactory
import com.rarible.protocol.erc20.api.client.Erc20IndexerApiServiceUriProvider
import com.rarible.protocol.nft.api.client.NftActivityControllerApi
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.nft.api.client.NftDomainControllerApi
import com.rarible.protocol.nft.api.client.NftIndexerApiClientFactory
import com.rarible.protocol.nft.api.client.NftIndexerApiServiceUriProvider
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftLazyMintControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.order.api.client.AuctionActivityControllerApi
import com.rarible.protocol.order.api.client.AuctionControllerApi
import com.rarible.protocol.order.api.client.OrderActivityControllerApi
import com.rarible.protocol.order.api.client.OrderAdminControllerApi
import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.order.api.client.OrderIndexerApiClientFactory
import com.rarible.protocol.order.api.client.OrderIndexerApiServiceUriProvider
import com.rarible.protocol.order.api.client.OrderSignatureControllerApi
import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.core.UnionWebClientCustomizer
import com.rarible.protocol.union.core.service.router.ActiveBlockchain
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(CoreConfiguration::class)
@EnableConfigurationProperties(value = [EthIntegrationProperties::class])
class EthClientConfiguration(
    private val webClientCustomizer: UnionWebClientCustomizer,
    private val properties: EthIntegrationProperties,
) {

    @Bean
    fun ethBlockchains(): ActiveBlockchain {
        return ActiveBlockchain(properties.active)
    }

    @Bean
    fun ethClients(
        nftUriProvider: NftIndexerApiServiceUriProvider,
        orderUriProvider: OrderIndexerApiServiceUriProvider,
        erc20UriProvider: Erc20IndexerApiServiceUriProvider,
    ): EthClients {
        val nftClientFactory = NftIndexerApiClientFactory(nftUriProvider, webClientCustomizer)
        val orderClientFactory = OrderIndexerApiClientFactory(orderUriProvider, webClientCustomizer)
        val erc20ClientFactory = Erc20IndexerApiClientFactory(erc20UriProvider, webClientCustomizer)

        return properties.active.map {
            val blockchainName = it.name.lowercase()
            it to EthBlockchainClients(
                balanceControllerApi = erc20ClientFactory.createBalanceApiClient(blockchainName),

                nftItemControllerApi = nftClientFactory.createNftItemApiClient(blockchainName),
                nftLazyMintControllerApi = nftClientFactory.createNftMintApiClient(blockchainName),
                nftOwnershipControllerApi = nftClientFactory.createNftOwnershipApiClient(blockchainName),
                nftCollectionControllerApi = nftClientFactory.createNftCollectionApiClient(blockchainName),
                nftActivityControllerApi = nftClientFactory.createNftActivityApiClient(blockchainName),
                nftDomainControllerApi = nftClientFactory.createNftDomainApiClient(blockchainName),

                orderActivityControllerApi = orderClientFactory.createOrderActivityApiClient(blockchainName),
                orderControllerApi = orderClientFactory.createOrderApiClient(blockchainName),
                auctionControllerApi = orderClientFactory.createAuctionApiClient(blockchainName),
                orderSignatureControllerApi = orderClientFactory.createOrderSignatureApiClient(blockchainName),
                orderAdminControllerApi = orderClientFactory.createOrderAdminApiClient(blockchainName),
                auctionActivityControllerApi = orderClientFactory.createAuctionActivityApiClient(blockchainName),
            )
        }.associateBy({ it.first }, { it.second }).let { EthClients(it) }
    }
}

class EthClients(
    val clients: Map<BlockchainDto, EthBlockchainClients>
)

class EthBlockchainClients(
    val balanceControllerApi: BalanceControllerApi,

    val nftItemControllerApi: NftItemControllerApi,
    val nftLazyMintControllerApi: NftLazyMintControllerApi,
    val nftOwnershipControllerApi: NftOwnershipControllerApi,
    val nftCollectionControllerApi: NftCollectionControllerApi,
    val nftActivityControllerApi: NftActivityControllerApi,
    val nftDomainControllerApi: NftDomainControllerApi,

    val orderActivityControllerApi: OrderActivityControllerApi,
    val orderControllerApi: OrderControllerApi,
    val auctionControllerApi: AuctionControllerApi,
    val orderSignatureControllerApi: OrderSignatureControllerApi,
    val orderAdminControllerApi: OrderAdminControllerApi,
    val auctionActivityControllerApi: AuctionActivityControllerApi,
)
