package com.rarible.protocol.union.core

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("common.feature-flags")
data class FeatureFlags(
    // TODO remove this FF when market support Polygon
    val enablePolygonInApi: Boolean = true
)