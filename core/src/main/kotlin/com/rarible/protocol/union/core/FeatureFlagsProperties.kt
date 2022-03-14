package com.rarible.protocol.union.core

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties("common.feature-flags")
@ConstructorBinding
data class FeatureFlagsProperties(
    val enableRevertedActivityEventSending: Boolean = false,
    val enableOwnershipSourceEnrichment: Boolean = false,
    val enableItemLastSaleEnrichment: Boolean = true
)