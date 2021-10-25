package com.rarible.protocol.union.integration.ethereum

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["integration.polygon.enabled"], havingValue = "true")
annotation class PolygonConfiguration
