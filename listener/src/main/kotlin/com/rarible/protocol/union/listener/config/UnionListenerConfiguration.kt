package com.rarible.protocol.union.listener.config

import com.github.cloudyrock.spring.v5.EnableMongock
import com.rarible.core.task.EnableRaribleTask
import com.rarible.protocol.union.enrichment.configuration.EnrichmentConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@EnableRaribleTask
@EnableMongock
@Import(value = [EnrichmentConfiguration::class])
@EnableConfigurationProperties(value = [UnionListenerProperties::class])
class UnionListenerConfiguration
