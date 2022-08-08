package com.rarible.protocol.union.listener.clickhouse.repository

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.ClickHouseContainer
import org.testcontainers.utility.DockerImageName
import java.lang.annotation.Inherited

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@ExtendWith(ClickHouseTestExtension::class)
annotation class ClickHouseTest

class ClickHouseTestExtension : BeforeAllCallback {

    override fun beforeAll(context: ExtensionContext) {
        System.setProperty(
            "clickhouse.host", clickHouseTestContainer.host
        )
        System.setProperty(
            "clickhouse.port", clickHouseTestContainer.port.toString()
        )
    }

    companion object {
        val clickHouseTestContainer = ClickHouseTestContainer()
    }
}

class ClickHouseTestContainer {

    val host: String = clickHouse.containerIpAddress

    val port: Int = clickHouse.getMappedPort(CLICKHOUSE_EXPOSED_PORT)

    companion object {
        const val CLICKHOUSE_EXPOSED_PORT = 8123
        val CLICKHOUSE_IMAGE: DockerImageName = DockerImageName.parse("yandex/clickhouse-server:22.1.3.7-alpine")

        @JvmStatic
        val clickHouse: ClickHouseContainer by lazy {
            ClickHouseContainer(CLICKHOUSE_IMAGE)
                .withExposedPorts(CLICKHOUSE_EXPOSED_PORT)
                .withReuse(true)
        }

        init {
            clickHouse.start()
        }
    }
}
