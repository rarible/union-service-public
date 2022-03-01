package com.rarible.protocol.union.integration.immutablex

import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexApiClient
import com.rarible.protocol.union.integration.immutablex.service.ImmutablexItemService
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.client.WebClient

@ImmutablexConfiguration
@Import(CoreConfiguration::class)
@EnableConfigurationProperties(ImmutablexIntegrationProperties::class)
class ImmutablexApiConfiguration {


    @Bean
    fun immutablexBlockchain() = BlockchainDto.IMMUTABLEX

    @Bean
    fun immutablexApiClient(
        webClient: WebClient,
        @Value("\${integration.immutablex.apiUrl:https://api.ropsten.x.immutable.com/v1}")
        apiUrl: String,
    ) = ImmutablexApiClient(webClient, apiUrl)

    @Bean
    fun immutablexItemService(client: ImmutablexApiClient): ImmutablexItemService = ImmutablexItemService(client)
}
