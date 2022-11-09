package com.rarible.protocol.union.integration.ethereum

import com.rarible.protocol.nft.api.client.NftIndexerApiClientFactory
import com.rarible.protocol.nft.api.client.NftIndexerApiServiceUriProvider
import com.rarible.protocol.order.api.client.OrderIndexerApiClientFactory
import com.rarible.protocol.order.api.client.OrderIndexerApiServiceUriProvider
import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.core.UnionWebClientCustomizer
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(CoreConfiguration::class)
@ComponentScan(basePackageClasses = [EthOrderConverter::class])
class EthApiFactoryConfiguration(
    private val webClientCustomizer: UnionWebClientCustomizer
) {

    @Bean
    fun ethNftIndexerApiClientFactory(uriProvider: NftIndexerApiServiceUriProvider): NftIndexerApiClientFactory {
        return NftIndexerApiClientFactory(uriProvider, webClientCustomizer)
    }

    @Bean
    fun ethOrderIndexerApiClientFactory(uriProvider: OrderIndexerApiServiceUriProvider): OrderIndexerApiClientFactory {
        return OrderIndexerApiClientFactory(uriProvider, webClientCustomizer)
    }

}