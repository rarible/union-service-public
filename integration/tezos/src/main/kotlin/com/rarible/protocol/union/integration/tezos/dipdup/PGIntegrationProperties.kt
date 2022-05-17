package com.rarible.protocol.union.integration.tezos.dipdup

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "integration.tezos.pg")
data class PGIntegrationProperties(
    val host: String,
    val port: Int,
    val user: String,
    val password: String,
    val database: String,
    val poolSize: Int = 10
)
