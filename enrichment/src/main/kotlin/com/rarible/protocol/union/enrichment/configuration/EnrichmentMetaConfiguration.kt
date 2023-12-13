package com.rarible.protocol.union.enrichment.configuration

import com.rarible.core.content.meta.loader.ApacheHttpContentReceiver
import com.rarible.core.content.meta.loader.ContentMetaReceiver
import com.rarible.core.content.meta.loader.ContentReceiver
import com.rarible.core.content.meta.loader.KtorApacheClientContentReceiver
import com.rarible.core.content.meta.loader.KtorCioClientContentReceiver
import com.rarible.core.meta.resource.detector.ContentDetector
import com.rarible.core.meta.resource.detector.embedded.EmbeddedContentDetector
import com.rarible.core.meta.resource.parser.UrlParser
import com.rarible.core.meta.resource.resolver.ConstantGatewayProvider
import com.rarible.core.meta.resource.resolver.IpfsGatewayResolver
import com.rarible.core.meta.resource.resolver.LegacyIpfsGatewaySubstitutor
import com.rarible.core.meta.resource.resolver.RandomGatewayProvider
import com.rarible.core.meta.resource.resolver.UrlResolver
import com.rarible.marketplace.generated.marketplacebackend.api.ApiClient
import com.rarible.marketplace.generated.marketplacebackend.api.client.CollectionControllerApi
import com.rarible.protocol.union.core.UnionWebClientCustomizer
import com.rarible.protocol.union.core.client.WebClientFactory
import com.rarible.protocol.union.core.util.safeSplit
import com.rarible.protocol.union.enrichment.ipfs.AlwaysSubstituteIpfsGatewayResolver
import com.rarible.protocol.union.enrichment.meta.UnionMetaPackage
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.web.reactive.function.client.WebClient

@ComponentScan(basePackageClasses = [UnionMetaPackage::class])
class EnrichmentMetaConfiguration(
    private val commonMetaProperties: CommonMetaProperties
) {

    @Bean
    fun embeddedContentProperties(): EmbeddedContentProperties {
        return commonMetaProperties.embedded
    }

    @Bean
    fun itemMetaTrimmingProperties(): MetaTrimmingProperties {
        return commonMetaProperties.trimming
    }

    @Bean
    fun contentDetector() = ContentDetector()

    @Bean
    fun embeddedContentDetector(contentDetector: ContentDetector) = EmbeddedContentDetector(contentDetector)

    @Bean
    fun urlParser() = UrlParser()

    @Bean
    fun urlResolver(): UrlResolver {
        val publicGatewayProvider = ConstantGatewayProvider(
            commonMetaProperties.ipfsPublicGateway.trimEnd('/')
        )
        val internalGatewayProvider = RandomGatewayProvider(
            safeSplit(commonMetaProperties.ipfsGateway).map { it.trimEnd('/') }
        )
        val customGatewaysResolver = if (commonMetaProperties.alwaysSubstituteIpfsGateway) {
            AlwaysSubstituteIpfsGatewayResolver()
        } else {
            LegacyIpfsGatewaySubstitutor(
                safeSplit(commonMetaProperties.ipfsLegacyGateway).map { it.trimEnd('/') }
            )
        }

        val ipfsGatewayResolver = IpfsGatewayResolver(
            publicGatewayProvider = publicGatewayProvider,
            internalGatewayProvider = internalGatewayProvider,
            customGatewaysResolver = customGatewaysResolver
        )

        return UrlResolver(ipfsGatewayResolver)
    }

    @Bean
    fun contentReceiver(): ContentReceiver {
        return when (commonMetaProperties.httpClient.type) {
            CommonMetaProperties.HttpClient.HttpClientType.KTOR_APACHE ->
                KtorApacheClientContentReceiver(
                    timeout = commonMetaProperties.httpClient.timeOut,
                    threadsCount = commonMetaProperties.httpClient.threadCount,
                    totalConnection = commonMetaProperties.httpClient.totalConnection,
                    keepAlive = commonMetaProperties.httpClient.keepAlive
                )

            CommonMetaProperties.HttpClient.HttpClientType.KTOR_CIO ->
                KtorCioClientContentReceiver(
                    timeout = commonMetaProperties.httpClient.timeOut,
                    threadsCount = commonMetaProperties.httpClient.threadCount,
                    totalConnection = commonMetaProperties.httpClient.totalConnection
                )

            CommonMetaProperties.HttpClient.HttpClientType.ASYNC_APACHE ->
                ApacheHttpContentReceiver(
                    timeout = commonMetaProperties.httpClient.timeOut,
                    connectionsPerRoute = commonMetaProperties.httpClient.connectionsPerRoute,
                    keepAlive = commonMetaProperties.httpClient.keepAlive
                )
        }
    }

    @Bean
    fun contentMetaReceiver(
        contentReceiver: ContentReceiver,
        commonMetaProperties: CommonMetaProperties,
        contentDetector: ContentDetector
    ): ContentMetaReceiver {
        return ContentMetaReceiver(
            contentReceiver = contentReceiver,
            maxBytes = commonMetaProperties.mediaFetchMaxSize.toInt(),
            contentDetector = contentDetector,
        )
    }

    @Bean
    fun simpleHashClient(webClientCustomizer: UnionWebClientCustomizer): WebClient {
        val props = commonMetaProperties.simpleHash
        val webClient = WebClientFactory.createClient(props.endpoint, mapOf("X-API-KEY" to props.apiKey))
        webClientCustomizer.customize(webClient)
        return webClient.build()
    }

    @Bean
    fun marketplaceCollectionClient(webClientCustomizer: UnionWebClientCustomizer): CollectionControllerApi {
        val url = commonMetaProperties.marketplace.endpoint
        val client = ApiClient(webClientCustomizer).setBasePath(url)
        return CollectionControllerApi(client)
    }
}
