package com.rarible.protocol.union.meta.loader.config

import com.rarible.protocol.union.enrichment.configuration.EnrichmentApiConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(value = [EnrichmentApiConfiguration::class])
class UnionMetaLoaderConfiguration
