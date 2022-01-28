package com.rarible.protocol.union.enrichment.configuration

import com.rarible.core.content.meta.loader.ContentMeta
import com.rarible.core.content.meta.loader.ContentMetaLoader
import com.rarible.core.lockredis.EnableRaribleRedisLock
import com.rarible.core.mongo.configuration.EnableRaribleMongo
import com.rarible.loader.cache.CacheLoaderService
import com.rarible.loader.cache.configuration.EnableRaribleCacheLoader
import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.core.event.OutgoingItemEventListener
import com.rarible.protocol.union.enrichment.meta.ContentMetaCacheLoader
import com.rarible.protocol.union.enrichment.meta.ContentMetaService
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import

@EnableRaribleCacheLoader
@EnableRaribleMongo
@EnableRaribleRedisLock
@EnableConfigurationProperties(MetaProperties::class)
@Import(CoreConfiguration::class)
@ComponentScan(
    basePackageClasses = [
        EnrichmentItemService::class,
        ItemRepository::class,
        OutgoingItemEventListener::class,
        ContentMetaService::class
    ]
)
class EnrichmentConfiguration {
    @Bean
    @Qualifier("content.meta.cache.loader.service")
    fun contentMetaCacheLoaderService(
        cacheLoaderServices: List<CacheLoaderService<*>>
    ): CacheLoaderService<ContentMeta> =
        @Suppress("UNCHECKED_CAST")
        (cacheLoaderServices.find { it.type == ContentMetaCacheLoader.TYPE } as CacheLoaderService<ContentMeta>)

    @Bean
    fun contentMetaLoader(
        metaProperties: MetaProperties
    ): ContentMetaLoader = ContentMetaLoader(
        mediaFetchTimeout = metaProperties.mediaFetchTimeout,
        mediaFetchMaxSize = metaProperties.mediaFetchMaxSize,
        openSeaProxyUrl = metaProperties.openSeaProxyUrl
    )
}
