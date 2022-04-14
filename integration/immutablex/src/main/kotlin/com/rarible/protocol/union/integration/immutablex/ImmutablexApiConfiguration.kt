package com.rarible.protocol.union.integration.immutablex

import com.rarible.protocol.union.api.ApiClient
import com.rarible.protocol.union.api.client.DefaultUnionWebClientCustomizer
import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.immutablex.client.EventsApi
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexApiClient
import com.rarible.protocol.union.integration.immutablex.converter.ImmutablexActivityConverter
import com.rarible.protocol.union.integration.immutablex.converter.ImmutablexOrderConverter
import com.rarible.protocol.union.integration.immutablex.service.ImmutablexActivityService
import com.rarible.protocol.union.integration.immutablex.service.ImmutablexItemService
import com.rarible.protocol.union.integration.immutablex.service.ImmutablexOrderService
import com.rarible.protocol.union.integration.immutablex.service.ImmutablexOwnershipService
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient


@ImmutablexConfiguration
@Import(CoreConfiguration::class)
@EnableConfigurationProperties(ImmutablexIntegrationProperties::class)
class ImmutablexApiConfiguration {


    @Bean
    fun immutablexBlockchain() = BlockchainDto.IMMUTABLEX

    @Bean
    fun immutablexApiClient(
        immutablexWebClient: WebClient,
    ) = ImmutablexApiClient(immutablexWebClient)

    @Bean
    fun immutablexWebClient(props: ImmutablexIntegrationProperties): WebClient {
        val mapper = ApiClient.createDefaultObjectMapper()
        val strategies = ExchangeStrategies
            .builder()
            .codecs { configurer: ClientCodecConfigurer ->
                configurer.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(mapper, MediaType.APPLICATION_JSON))
                configurer.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(mapper, MediaType.APPLICATION_JSON))
            }.build()
        val webClient = WebClient.builder().exchangeStrategies(strategies)

        DefaultUnionWebClientCustomizer().customize(webClient)
        webClient.defaultHeaders {
            it.add("x-api-key", props.apiKey)
        }

        return webClient.baseUrl(props.client!!.url!!).build()
    }

    @Bean
    fun immutablexItemService(client: ImmutablexApiClient): ImmutablexItemService = ImmutablexItemService(client)

    @Bean
    fun immutablexOrderConverter(): ImmutablexOrderConverter = ImmutablexOrderConverter()


    @Bean
    fun immutablexOrderService(
        client: ImmutablexApiClient,
        converter: ImmutablexOrderConverter,
    ): ImmutablexOrderService = ImmutablexOrderService(client, converter)

    @Bean
    fun eventsApi(immutablexWebClient: WebClient) = EventsApi(immutablexWebClient)


    @Bean
    fun immutablexActivityConverter(orderService: ImmutablexOrderService): ImmutablexActivityConverter =
        ImmutablexActivityConverter(orderService)

    @Bean
    fun immutablesActivityService(
        client: ImmutablexApiClient,
        converter: ImmutablexActivityConverter,
    ): ImmutablexActivityService = ImmutablexActivityService(client, converter)

    @Bean
    fun immutablexOwnershipService(immutablexApiClient: ImmutablexApiClient): ImmutablexOwnershipService =
        ImmutablexOwnershipService(immutablexApiClient)

}
