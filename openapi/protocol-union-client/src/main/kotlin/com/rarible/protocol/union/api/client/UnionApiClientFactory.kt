package com.rarible.protocol.union.api.client

import com.rarible.protocol.client.NoopWebClientCustomizer
import com.rarible.protocol.union.api.ApiClient
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer

open class UnionApiClientFactory(
    private val uriProvider: UnionApiServiceUriProvider,
    private val webClientCustomizer: WebClientCustomizer = NoopWebClientCustomizer()
) {

    fun createTestUnionApiClient(): TestControllerApi {
        return TestControllerApi(createApiClient())
    }

    private fun createApiClient(): ApiClient {
        return ApiClient(webClientCustomizer)
            .setBasePath(uriProvider.getUri().toASCIIString())
    }

}

