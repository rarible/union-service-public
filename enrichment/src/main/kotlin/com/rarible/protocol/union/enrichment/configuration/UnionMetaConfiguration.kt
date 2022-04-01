package com.rarible.protocol.union.enrichment.configuration

import com.rarible.core.content.meta.loader.*
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
        }
    }

    @Bean
    fun contentMetaReceiver(
        contentReceiver: ContentReceiver,
        unionMetaProperties: UnionMetaProperties,
        meterRegistry: MeterRegistry
    ): ContentMetaReceiver {
        val measurableContentReceiver = MeasurableContentReceiver(contentReceiver, meterRegistry)
        return ContentMetaReceiver(
            contentReceiver = if (unionMetaProperties.httpClient.resetOnError)
                ResetableContentReceiver(measurableContentReceiver) else measurableContentReceiver,
            maxBytes = unionMetaProperties.mediaFetchMaxSize.toInt(),
            contentReceiverMetrics = ContentReceiverMetrics(meterRegistry)
        )
    }

    @Bean
    @Qualifier("union.meta.cache.loader.service")
    fun unionMetaCacheLoaderService(
        cacheLoaderServices: List<CacheLoaderService<*>>
    ): CacheLoaderService<UnionMeta> =
        @Suppress("UNCHECKED_CAST")
        (cacheLoaderServices.find { it.type == UnionMetaCacheLoader.TYPE } as CacheLoaderService<UnionMeta>)

}
