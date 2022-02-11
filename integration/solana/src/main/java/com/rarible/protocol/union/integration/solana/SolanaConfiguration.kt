package com.rarible.protocol.union.integration.solana

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["integration.solana.enabled"], havingValue = "true")
annotation class SolanaConfiguration
