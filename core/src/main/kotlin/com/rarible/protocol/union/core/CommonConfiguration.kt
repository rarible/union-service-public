package com.rarible.protocol.union.core

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CommonConfiguration(
    private val properties: CommonProperties
) {

    // Can't be created in CoreConfiguration (circular reference)
    @Bean
    fun featureFlags(): FeatureFlagsProperties = properties.featureFlags
}
