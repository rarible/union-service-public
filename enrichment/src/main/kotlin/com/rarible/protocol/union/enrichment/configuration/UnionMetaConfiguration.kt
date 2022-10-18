package com.rarible.protocol.union.enrichment.configuration

import com.rarible.core.content.meta.loader.ApacheHttpContentReceiver
import com.rarible.core.content.meta.loader.ContentMetaReceiver
import com.rarible.core.content.meta.loader.ContentReceiver
import com.rarible.core.content.meta.loader.ContentReceiverMetrics
import com.rarible.core.content.meta.loader.KtorApacheClientContentReceiver
import com.rarible.core.content.meta.loader.KtorCioClientContentReceiver
import com.rarible.core.content.meta.loader.MeasurableContentReceiver
import com.rarible.core.meta.resource.detector.ContentDetector
import com.rarible.core.meta.resource.detector.embedded.EmbeddedContentDetector
import com.rarible.core.meta.resource.parser.UrlParser
import com.rarible.core.meta.resource.resolver.ConstantGatewayProvider
import com.rarible.core.meta.resource.resolver.IpfsGatewayResolver
import com.rarible.core.meta.resource.resolver.LegacyIpfsGatewaySubstitutor
import com.rarible.core.meta.resource.resolver.RandomGatewayProvider
import com.rarible.core.meta.resource.resolver.UrlResolver
import com.rarible.loader.cache.CacheLoaderService
import com.rarible.loader.cache.configuration.EnableRaribleCacheLoader
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.util.safeSplit
import com.rarible.protocol.union.enrichment.meta.UnionMetaPackage
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaDownloader
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan

@EnableRaribleCacheLoader
@EnableConfigurationProperties(UnionMetaProperties::class)
@ComponentScan(
    basePackageClasses = [
        UnionMetaPackage::class
    ]
)
class UnionMetaConfiguration(
    private val unionMetaProperties: UnionMetaProperties
) {

    @Bean
    fun embeddedContentProperties(): EmbeddedContentProperties {
        return unionMetaProperties.embedded
    }

    @Bean
    fun itemMetaTrimmingProperties(): ItemMetaTrimmingProperties {
        return unionMetaProperties.trimming
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
            unionMetaProperties.ipfsPublicGateway.trimEnd('/')
        )
        val internalGatewayProvider = RandomGatewayProvider(
            safeSplit(unionMetaProperties.ipfsGateway).map { it.trimEnd('/') }
        )
        val legacyGatewayResolver = LegacyIpfsGatewaySubstitutor(
            safeSplit(unionMetaProperties.ipfsLegacyGateway).map { it.trimEnd('/') }
        )

        val ipfsGatewayResolver = IpfsGatewayResolver(
            publicGatewayProvider = publicGatewayProvider,
            internalGatewayProvider = internalGatewayProvider,
            customGatewaysResolver = legacyGatewayResolver
        )

        return UrlResolver(ipfsGatewayResolver)
    }

    @Bean
    fun contentReceiver(): ContentReceiver {
        return when (unionMetaProperties.httpClient.type) {
            UnionMetaProperties.HttpClient.HttpClientType.KTOR_APACHE ->
                KtorApacheClientContentReceiver(
                    timeout = unionMetaProperties.httpClient.timeOut,
                    threadsCount = unionMetaProperties.httpClient.threadCount,
                    totalConnection = unionMetaProperties.httpClient.totalConnection,
                    keepAlive = unionMetaProperties.httpClient.keepAlive
                )

            UnionMetaProperties.HttpClient.HttpClientType.KTOR_CIO ->
                KtorCioClientContentReceiver(
                    timeout = unionMetaProperties.httpClient.timeOut,
                    threadsCount = unionMetaProperties.httpClient.threadCount,
                    totalConnection = unionMetaProperties.httpClient.totalConnection
                )

            UnionMetaProperties.HttpClient.HttpClientType.ASYNC_APACHE ->
                ApacheHttpContentReceiver(
                    timeout = unionMetaProperties.httpClient.timeOut,
                    connectionsPerRoute = unionMetaProperties.httpClient.connectionsPerRoute,
                    keepAlive = unionMetaProperties.httpClient.keepAlive
                )
        }
    }

    @Bean
    fun contentMetaReceiver(
        contentReceiver: ContentReceiver,
        unionMetaProperties: UnionMetaProperties,
        meterRegistry: MeterRegistry,
        contentDetector: ContentDetector
    ): ContentMetaReceiver {
        return ContentMetaReceiver(
            contentReceiver = MeasurableContentReceiver(contentReceiver, meterRegistry),
            maxBytes = unionMetaProperties.mediaFetchMaxSize.toInt(),
            contentReceiverMetrics = ContentReceiverMetrics(meterRegistry),
            contentDetector = contentDetector,
        )
    }

    @Bean
    @Qualifier("union.meta.cache.loader.service")
    fun unionMetaCacheLoaderService(
        cacheLoaderServices: List<CacheLoaderService<*>>
    ): CacheLoaderService<UnionMeta> =
        @Suppress("UNCHECKED_CAST")
        (cacheLoaderServices.find { it.type == ItemMetaDownloader.TYPE } as CacheLoaderService<UnionMeta>)

}
