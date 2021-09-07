package com.rarible.protocol.union.core

import com.rarible.protocol.flow.nft.api.client.FlowNftItemControllerApi
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.union.core.ethereum.service.EthereumItemService
import com.rarible.protocol.union.core.flow.service.FlowItemService
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.dto.FlowBlockchainDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackageClasses = [CoreConfiguration::class])
class CoreConfiguration {

    @Bean
    fun ethereumItemService(@Qualifier("ethereum.item.api") ethereumNftItemApi: NftItemControllerApi): ItemService {
        return EthereumItemService(EthBlockchainDto.ETHEREUM, ethereumNftItemApi)
    }

    @Bean
    fun polygonItemService(@Qualifier("polygon.item.api") ethereumNftItemApi: NftItemControllerApi): ItemService {
        return EthereumItemService(EthBlockchainDto.POLYGON, ethereumNftItemApi)
    }

    @Bean
    fun flowItemService(flowNftItemApi: FlowNftItemControllerApi): ItemService {
        return FlowItemService(FlowBlockchainDto.FLOW, flowNftItemApi)
    }

}