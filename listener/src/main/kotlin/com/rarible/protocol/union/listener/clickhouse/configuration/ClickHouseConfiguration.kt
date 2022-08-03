package com.rarible.protocol.union.listener.clickhouse.configuration

import com.clickhouse.client.ClickHouseClient
import com.clickhouse.client.ClickHouseClientBuilder
import com.clickhouse.client.ClickHouseCredentials
import com.clickhouse.client.ClickHouseFormat
import com.clickhouse.client.ClickHouseNode
import com.clickhouse.client.ClickHouseNodeSelector
import com.clickhouse.client.ClickHouseProtocol
import com.clickhouse.client.config.ClickHouseClientOption
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(ClickHouseProperties::class)
class ClickHouseConfiguration(
    private val clickHouseProperties: ClickHouseProperties
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun clickHouseClientBuilder(): ClickHouseClientBuilder {
        val user = clickHouseProperties.user
        val password = clickHouseProperties.password
        logger.info("Create ClickHouse client builder with user=$user")

        return ClickHouseClient.builder()
            .nodeSelector(ClickHouseNodeSelector.of(ClickHouseProtocol.HTTP))
            .defaultCredentials(ClickHouseCredentials.fromUserAndPassword(user, password))
            .option(ClickHouseClientOption.FORMAT, ClickHouseFormat.RowBinaryWithNamesAndTypes)
    }

    @Bean
    fun clickHouseNode(): ClickHouseNode {
        val host = clickHouseProperties.host
        val port = clickHouseProperties.port
        val database = clickHouseProperties.database
        logger.info("Create ClickHouse node with host=$host, port=$port, database=$database")

        return ClickHouseNode.of(host, ClickHouseProtocol.HTTP, port, database)
    }
}
