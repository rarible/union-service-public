package com.rarible.protocol.union.listener.clickhouse.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("clickhouse")
data class ClickHouseProperties(
    val host: String,
    val port: Int,
    val database: String,
    val user: String,
    val password: String,
)
