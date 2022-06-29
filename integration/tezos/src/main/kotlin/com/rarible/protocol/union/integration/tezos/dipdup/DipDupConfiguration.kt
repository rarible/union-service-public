package com.rarible.protocol.union.integration.tezos.dipdup

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["integration.tezos.dipdup.enabled"], havingValue = "true")
annotation class DipDupConfiguration
