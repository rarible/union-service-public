package com.rarible.protocol.union.meta.loader.test

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.configuration.UnionMetaProperties
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest


@EnableAutoConfiguration
@IntegrationTest
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = [
        "application.environment = test",
        "spring.cloud.consul.config.enabled = false",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "logging.logjson.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
        "meta.simpleHash.enabled = true"
    ]
)
class SHSupportedBlockchainFt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var props: UnionMetaProperties

    @Test
    fun `check supported chains from enrichment config`() = runBlocking<Unit> {
        assertThat(props.simpleHash.enabled).isTrue()
        assertThat(props.simpleHash.supported).hasSize(1)
        assertThat(props.simpleHash.supported).contains(BlockchainDto.ETHEREUM)
    }
}
