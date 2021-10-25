package com.rarible.protocol.union.integration.tezos

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["integration.tezos.enabled"], havingValue = "true")
annotation class TezosConfiguration
