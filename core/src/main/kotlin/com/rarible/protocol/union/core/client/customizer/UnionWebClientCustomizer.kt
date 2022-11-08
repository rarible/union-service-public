package com.rarible.protocol.union.core.client.customizer

import com.rarible.protocol.union.core.FeatureFlagsProperties
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class UnionWebClientCustomizer(
    ff: FeatureFlagsProperties
) : WebClientCustomizer {

    private val customizer = if (ff.enableWebClientConnectionPool) {
        UnionWebClientPoolCustomizer
    } else {
        UnionWebClientNoPoolCustomizer
    }

    override fun customize(webClientBuilder: WebClient.Builder?) {
        return customizer.customize(webClientBuilder)
    }

}