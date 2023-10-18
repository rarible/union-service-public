package com.rarible.protocol.union.core

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("common")
data class CommonProperties(
    val featureFlags: FeatureFlagsProperties = FeatureFlagsProperties()
)
