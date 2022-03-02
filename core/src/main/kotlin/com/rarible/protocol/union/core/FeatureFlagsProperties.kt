package com.rarible.protocol.union.core

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("common.feature-flags")
data class FeatureFlagsProperties(
    val enableRevertedActivityEvents: Boolean = false
)