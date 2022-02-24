package com.rarible.protocol.union.meta.loader.config

import com.rarible.protocol.union.enrichment.configuration.UnionMetaConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(value = [UnionMetaConfiguration::class])
class UnionMetaLoaderConfiguration
