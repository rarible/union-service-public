package com.rarible.protocol.union.listener.clickhouse.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConditionalOnClickhouseEnabled
@ConfigurationProperties("clickhouse")
data class ClickHouseProperties(
    val host: String = "localhost",
    val port: Int = 0,
    val database: String,
    val user: String,
    val password: String,
)
