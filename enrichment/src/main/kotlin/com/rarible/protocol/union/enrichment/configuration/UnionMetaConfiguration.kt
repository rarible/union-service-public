package com.rarible.protocol.union.enrichment.configuration

import com.rarible.core.content.meta.loader.ContentMetaLoader
import com.rarible.loader.cache.CacheLoaderService
import com.rarible.loader.cache.configuration.EnableRaribleCacheLoader
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.enrichment.meta.UnionMetaCacheLoader
import com.rarible.protocol.union.enrichment.meta.UnionMetaPackage
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
    fun contentMetaLoader(
        unionMetaProperties: UnionMetaProperties
    ): ContentMetaLoader = ContentMetaLoader(
        mediaFetchTimeout = unionMetaProperties.mediaFetchTimeout,
        mediaFetchMaxSize = unionMetaProperties.mediaFetchMaxSize,
        openSeaProxyUrl = unionMetaProperties.openSeaProxyUrl
    )

    @Bean
    @Qualifier("union.meta.cache.loader.service")
    fun unionMetaCacheLoaderService(
        cacheLoaderServices: List<CacheLoaderService<*>>
    ): CacheLoaderService<UnionMeta> =
        @Suppress("UNCHECKED_CAST")
        (cacheLoaderServices.find { it.type == UnionMetaCacheLoader.TYPE } as CacheLoaderService<UnionMeta>)
}
