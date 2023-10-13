package com.rarible.protocol.union.integration.solana

import com.rarible.protocol.union.core.DefaultConsumerProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "integration.solana")
class SolanaIntegrationProperties(
    val enabled: Boolean,
    val consumer: DefaultConsumerProperties?,
)
