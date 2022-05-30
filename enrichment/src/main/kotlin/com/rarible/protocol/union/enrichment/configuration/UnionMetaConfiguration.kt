package com.rarible.protocol.union.enrichment.configuration

import com.rarible.core.content.meta.loader.ApacheHttpContentReceiver
import com.rarible.core.content.meta.loader.ContentMetaReceiver
import com.rarible.core.content.meta.loader.ContentReceiver
import com.rarible.core.content.meta.loader.ContentReceiverMetrics
import com.rarible.core.content.meta.loader.KtorApacheClientContentReceiver
import com.rarible.core.content.meta.loader.KtorCioClientContentReceiver
import com.rarible.core.content.meta.loader.MeasurableContentReceiver
import com.rarible.core.meta.resource.ArweaveUrl
import com.rarible.core.meta.resource.ConstantGatewayProvider
import com.rarible.core.meta.resource.CustomIpfsGatewayResolver
import com.rarible.core.meta.resource.GatewayProvider
import com.rarible.core.meta.resource.LegacyIpfsGatewaySubstitutor
import com.rarible.core.meta.resource.RandomGatewayProvider
import com.rarible.core.meta.resource.cid.CidV1Validator
import com.rarible.core.meta.resource.detector.core.ContentMetaDetectProcessor
import com.rarible.core.meta.resource.detector.core.DefaultContentMetaDetectorProvider
import com.rarible.core.meta.resource.detector.core.ExifDetector
import com.rarible.core.meta.resource.detector.core.HtmlDetector
import com.rarible.core.meta.resource.detector.core.PngDetector
import com.rarible.core.meta.resource.detector.core.SvgDetector
import com.rarible.core.meta.resource.detector.embedded.DefaultEmbeddedContentDecoderProvider
import com.rarible.core.meta.resource.detector.embedded.EmbeddedBase64Decoder
import com.rarible.core.meta.resource.detector.embedded.EmbeddedContentDetectProcessor
import com.rarible.core.meta.resource.detector.embedded.EmbeddedSvgDecoder
import com.rarible.core.meta.resource.parser.ArweaveUrlResourceParser
import com.rarible.core.meta.resource.parser.CidUrlResourceParser
import com.rarible.core.meta.resource.parser.DefaultUrlResourceParserProvider
import com.rarible.core.meta.resource.parser.HttpUrlResourceParser
import com.rarible.core.meta.resource.parser.UrlResourceParsingProcessor
import com.rarible.core.meta.resource.parser.ipfs.AbstractIpfsUrlResourceParser
import com.rarible.core.meta.resource.parser.ipfs.ForeignIpfsUrlResourceParser
import com.rarible.core.meta.resource.resolver.ArweaveGatewayResolver
import com.rarible.core.meta.resource.resolver.IpfsCidGatewayResolver
import com.rarible.core.meta.resource.resolver.IpfsGatewayResolver
import com.rarible.core.meta.resource.resolver.SimpleHttpGatewayResolver
import com.rarible.core.meta.resource.resolver.UrlResolver
import com.rarible.loader.cache.CacheLoaderService
import com.rarible.loader.cache.configuration.EnableRaribleCacheLoader
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.enrichment.meta.UnionMetaCacheLoader
import com.rarible.protocol.union.enrichment.meta.UnionMetaPackage
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
class UnionMetaConfiguration {

    @Bean
    fun contentReceiver(
        unionMetaProperties: UnionMetaProperties
    ): ContentReceiver {
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
    fun contentMetaDetectProcessor(): ContentMetaDetectProcessor {
        return ContentMetaDetectProcessor(
            provider = DefaultContentMetaDetectorProvider(
                htmlDetector = HtmlDetector,
                svgDetector = SvgDetector,
                pngDetector = PngDetector,
                exifDetector = ExifDetector
            )
        )
    }

    @Bean
    fun contentMetaReceiver(
        contentReceiver: ContentReceiver,
        unionMetaProperties: UnionMetaProperties,
        meterRegistry: MeterRegistry,
        contentMetaDetectProcessor: ContentMetaDetectProcessor
    ): ContentMetaReceiver {
        return ContentMetaReceiver(
            contentReceiver = MeasurableContentReceiver(contentReceiver, meterRegistry),
            maxBytes = unionMetaProperties.mediaFetchMaxSize.toInt(),
            contentReceiverMetrics = ContentReceiverMetrics(meterRegistry),
            contentMetaDetectProcessor = contentMetaDetectProcessor,
        )
    }

    @Bean
    @Qualifier("union.meta.cache.loader.service")
    fun unionMetaCacheLoaderService(
        cacheLoaderServices: List<CacheLoaderService<*>>
    ): CacheLoaderService<UnionMeta> =
        @Suppress("UNCHECKED_CAST")
        (cacheLoaderServices.find { it.type == UnionMetaCacheLoader.TYPE } as CacheLoaderService<UnionMeta>)

    @Bean
    fun publicGatewayProvider(unionMetaProperties: UnionMetaProperties): GatewayProvider {
        return ConstantGatewayProvider(
            unionMetaProperties.ipfsPublicGateway.trimEnd('/')
        )
    }

    @Bean
    fun innerGatewaysProvider(unionMetaProperties: UnionMetaProperties): GatewayProvider {
        return RandomGatewayProvider(
            unionMetaProperties.ipfsGateway.split(",").map { it.trimEnd('/') }
        )
    }

    @Bean
    fun legacyGatewaysProvider(unionMetaProperties: UnionMetaProperties): CustomIpfsGatewayResolver {
        return LegacyIpfsGatewaySubstitutor(
            unionMetaProperties.ipfsLegacyGateway?.split(",")?.map { it.trimEnd('/') } ?: emptyList()
        )
    }

    @Bean
    fun urlResourceProcessor(): UrlResourceParsingProcessor {
        val cidOneValidator = CidV1Validator()
        val foreignIpfsUrlResourceParser = ForeignIpfsUrlResourceParser(
            cidOneValidator = cidOneValidator
        )

        val defaultUrlResourceParserProvider = DefaultUrlResourceParserProvider(
            arweaveUrlParser = ArweaveUrlResourceParser(),
            foreignIpfsUrlResourceParser = foreignIpfsUrlResourceParser,
            abstractIpfsUrlResourceParser = AbstractIpfsUrlResourceParser(),
            cidUrlResourceParser = CidUrlResourceParser(cidOneValidator),
            httpUrlParser = HttpUrlResourceParser()
        )

        return UrlResourceParsingProcessor(
            provider = defaultUrlResourceParserProvider
        )
    }

    @Bean
    fun urlResolver(
        publicGatewayProvider: GatewayProvider,
        innerGatewaysProvider: GatewayProvider,
        legacyGatewaysProvider: CustomIpfsGatewayResolver
    ): UrlResolver {
        val arweaveGatewayProvider = ConstantGatewayProvider(ArweaveUrl.ARWEAVE_GATEWAY)

        val ipfsGatewayResolver = IpfsGatewayResolver(
            publicGatewayProvider = publicGatewayProvider,
            innerGatewaysProvider = innerGatewaysProvider,
            customGatewaysResolver = legacyGatewaysProvider
        )

        val ipfsCidGatewayResolver = IpfsCidGatewayResolver(
            publicGatewayProvider = publicGatewayProvider,
            innerGatewaysProvider = innerGatewaysProvider,
        )

        val arweaveGatewayResolver = ArweaveGatewayResolver(
            arweaveGatewayProvider = arweaveGatewayProvider
        )

        return UrlResolver(
            ipfsGatewayResolver = ipfsGatewayResolver,
            ipfsCidGatewayResolver = ipfsCidGatewayResolver,
            arweaveGatewayResolver = arweaveGatewayResolver,
            simpleHttpGatewayResolver = SimpleHttpGatewayResolver()
        )
    }

    @Bean
    fun embeddedContentDetectProcessor() : EmbeddedContentDetectProcessor =
        EmbeddedContentDetectProcessor(
            provider = DefaultEmbeddedContentDecoderProvider(
                embeddedBase64Decoder = EmbeddedBase64Decoder,
                embeddedSvgDecoder = EmbeddedSvgDecoder
            )
        )
}
