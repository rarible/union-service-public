package com.rarible.protocol.union.enrichment.configuration

import com.rarible.core.mongo.configuration.EnableRaribleMongo
import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.enrichment.event.ItemEventListener
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import

@EnableRaribleMongo
@Import(CoreConfiguration::class)
@ComponentScan(
    basePackageClasses = [
        EnrichmentItemService::class,
        ItemRepository::class,
        ItemEventListener::class
    ]
)
class EnrichmentConfiguration
