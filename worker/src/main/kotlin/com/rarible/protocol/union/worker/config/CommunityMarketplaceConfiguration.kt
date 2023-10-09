package com.rarible.protocol.union.worker.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.marketplace.generated.whitelabelinternal.api.ApiClient
import com.rarible.marketplace.generated.whitelabelinternal.api.RFC3339DateFormat
import com.rarible.marketplace.generated.whitelabelinternal.api.client.MarketplacesControllerApi
import com.rarible.protocol.union.core.UnionWebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.TimeZone

@Configuration
class CommunityMarketplaceConfiguration(
    val properties: WorkerProperties
) {
    @Bean
    fun apiClient(
        webClientCustomizer: UnionWebClientCustomizer,
        objectMapper: ObjectMapper,
    ): ApiClient {
        val dateFormat = RFC3339DateFormat()
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return ApiClient(
            webClientCustomizer,
            objectMapper,
            dateFormat
        )
            .setBasePath(properties.communityMarketplace.communityMarketplaceUrl)
    }

    @Bean
    fun communityMarketplaceApi(apiClient: ApiClient) = MarketplacesControllerApi(apiClient)
}
