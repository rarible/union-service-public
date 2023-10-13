package com.rarible.protocol.union.integration.ethereum

import com.rarible.protocol.union.core.service.router.BlockchainService
import com.rarible.protocol.union.core.service.router.EvmActivityService
import com.rarible.protocol.union.core.service.router.EvmAuctionService
import com.rarible.protocol.union.core.service.router.EvmBalanceService
import com.rarible.protocol.union.core.service.router.EvmCollectionService
import com.rarible.protocol.union.core.service.router.EvmDomainService
import com.rarible.protocol.union.core.service.router.EvmItemService
import com.rarible.protocol.union.core.service.router.EvmOrderService
import com.rarible.protocol.union.core.service.router.EvmOwnershipService
import com.rarible.protocol.union.core.service.router.EvmSignatureService
import com.rarible.protocol.union.dto.BlockchainDto
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
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(EthClientConfiguration::class)
@EnableConfigurationProperties(value = [EthIntegrationProperties::class])
@ComponentScan(basePackageClasses = [EthOrderConverter::class])
class EthApiConfiguration(
    private val properties: EthIntegrationProperties,
    private val ethClients: EthClients
) {

    @Bean
    fun ethBalanceService(): EvmBalanceService {
        return evmServices { blockchain, clients ->
            EthBalanceService(
                blockchain = blockchain,
                balanceControllerApi = clients.balanceControllerApi
            )
        }.let { EvmBalanceService(it) }
    }

    @Bean
    fun ethItemService(): EvmItemService {
        return evmServices { blockchain, clients ->
            EthItemService(
                blockchain = blockchain,
                itemControllerApi = clients.nftItemControllerApi,
                lazyItemControllerApi = clients.nftLazyMintControllerApi
            )
        }.let { EvmItemService(it) }
    }

    @Bean
    fun ethOwnershipService(): EvmOwnershipService {
        return evmServices { blockchain, clients ->
            EthOwnershipService(
                blockchain = blockchain,
                ownershipControllerApi = clients.nftOwnershipControllerApi
            )
        }.let { EvmOwnershipService(it) }
    }

    @Bean
    fun ethCollectionService(): EvmCollectionService {
        return evmServices { blockchain, clients ->
            EthCollectionService(
                blockchain = blockchain,
                collectionControllerApi = clients.nftCollectionControllerApi
            )
        }.let { EvmCollectionService(it) }
    }

    @Bean
    fun ethOrderService(
        ethOrderConverter: EthOrderConverter
    ): EvmOrderService {
        return evmServices { blockchain, clients ->
            EthOrderService(
                blockchain = blockchain,
                orderAdminControllerApi = clients.orderAdminControllerApi,
                orderControllerApi = clients.orderControllerApi,
                ethOrderConverter = ethOrderConverter,
                properties = properties.blockchains[blockchain]!!
            )
        }.let { EvmOrderService(it) }
    }

    @Bean
    fun ethAuctionService(
        ethAuctionConverter: EthAuctionConverter
    ): EvmAuctionService {
        return evmServices { blockchain, clients ->
            EthAuctionService(
                blockchain = blockchain,
                auctionControllerApi = clients.auctionControllerApi,
                ethAuctionConverter = ethAuctionConverter,
                properties = properties.blockchains[blockchain]!!
            )
        }.let { EvmAuctionService(it) }
    }

    @Bean
    fun ethSignatureService(): EvmSignatureService {
        return evmServices { blockchain, clients ->
            EthSignatureService(
                blockchain = blockchain,
                signatureControllerApi = clients.orderSignatureControllerApi
            )
        }.let { EvmSignatureService(it) }
    }

    @Bean
    fun ethDomainService(): EvmDomainService {
        return evmServices { blockchain, clients ->
            EthDomainService(
                blockchain = blockchain,
                nftDomainControllerApi = clients.nftDomainControllerApi
            )
        }.let { EvmDomainService(it) }
    }

    @Bean
    fun ethActivityService(
        ethActivityConverter: EthActivityConverter
    ): EvmActivityService {
        return evmServices { blockchain, clients ->
            EthActivityService(
                blockchain = blockchain,
                activityItemControllerApi = clients.nftActivityControllerApi,
                activityOrderControllerApi = clients.orderActivityControllerApi,
                activityAuctionControllerApi = clients.auctionActivityControllerApi,
                ethActivityConverter = ethActivityConverter
            )
        }.let { EvmActivityService(it) }
    }

    private fun <T : BlockchainService> evmServices(
        block: (blockchain: BlockchainDto, clients: EthBlockchainClients) -> T
    ): List<T> {
        return properties.active.map { block(it, ethClients.clients[it]!!) }
    }
}
