package com.rarible.protocol.union.enrichment.configuration

import com.rarible.core.cache.EnableRaribleCache
import com.rarible.core.lockredis.EnableRaribleRedisLock
import com.rarible.core.mongo.configuration.EnableRaribleMongo
import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.core.event.OutgoingItemEventListener
import com.rarible.protocol.union.enrichment.meta.MediaMetaService
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import

@EnableRaribleMongo
@EnableRaribleCache
@EnableRaribleRedisLock
@EnableConfigurationProperties(MetaProperties::class)
@Import(CoreConfiguration::class)
@ComponentScan(
    basePackageClasses = [
        EnrichmentItemService::class,
        ItemRepository::class,
        OutgoingItemEventListener::class,
        MediaMetaService::class
    ]
)
class EnrichmentConfiguration
