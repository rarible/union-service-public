package com.rarible.protocol.union.integration.immutablex

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@ConditionalOnProperty(name = ["integration.immutablex.enabled"], havingValue = "true")
annotation class ImmutablexConfiguration
