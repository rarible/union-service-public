package com.rarible.protocol.union.enrichment.configuration

import com.rarible.core.lockredis.EnableRaribleRedisLock
import com.rarible.core.mongo.configuration.EnableRaribleMongo
import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.core.event.OutgoingItemEventListener
import com.rarible.protocol.union.enrichment.meta.UnionMetaPackage
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.service.EnrichmentServicePackage
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import

@EnableRaribleMongo
@EnableRaribleRedisLock
@Import(CoreConfiguration::class)
@ComponentScan(
    basePackageClasses = [
        EnrichmentServicePackage::class,
        ItemRepository::class,
        OutgoingItemEventListener::class
    ]
)
class EnrichmentConfiguration
