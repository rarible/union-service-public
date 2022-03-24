package com.rarible.protocol.union.integration.solana

import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.solana.converter.SolanaItemConverter
import com.rarible.protocol.union.integration.solana.service.SolanaItemService
import com.rarible.protocol.union.integration.solana.service.SolanaOwnershipService
import com.rarible.solana.protocol.api.client.BalanceControllerApi
import com.rarible.solana.protocol.api.client.SolanaNftIndexerApiClientFactory
import com.rarible.solana.protocol.api.client.TokenControllerApi
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import

@SolanaConfiguration
@Import(CoreConfiguration::class)
@ComponentScan(basePackageClasses = [SolanaItemConverter::class])
@EnableConfigurationProperties(value = [SolanaIntegrationProperties::class])
class SolanaApiConfiguration {

    @Bean
    fun solanaBlockchain(): BlockchainDto = BlockchainDto.SOLANA

    //-------------------- API --------------------//
    @Bean
    fun solanaTokenApi(factory: SolanaNftIndexerApiClientFactory): TokenControllerApi =
        factory.createTokenControllerApiClient()

    @Bean
    fun solanaBalanceApi(factory: SolanaNftIndexerApiClientFactory): BalanceControllerApi =
        factory.createBalanceControllerApiClient()

    //-------------------- Services --------------------//
    @Bean
    fun solanaItemService(controllerApi: TokenControllerApi): SolanaItemService {
        return SolanaItemService(controllerApi)
    }

    @Bean
    fun solanaOwnershipService(controllerApi: BalanceControllerApi): SolanaOwnershipService {
        return SolanaOwnershipService(controllerApi)
    }
}
