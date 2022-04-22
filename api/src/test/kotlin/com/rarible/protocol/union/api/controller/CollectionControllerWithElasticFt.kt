package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.api.service.CollectionQueryService
import com.rarible.protocol.union.api.service.elastic.CollectionElasticService
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@IntegrationTest
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "application.environment = test",
        "spring.cloud.consul.config.enabled = false",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
        "common.feature-flags.enableCollectionQueriesToElastic = true"
    ]
)
class CollectionControllerWithElasticFt: AbstractIntegrationTest() {

    @Autowired
    private lateinit var queryService: CollectionQueryService

    @Test
    internal fun `check query service, should be elastic`() {
        assertTrue(
            queryService is CollectionElasticService,
            """
                Query service class is wrong! Actual ${queryService::class.qualifiedName},
                should be ${CollectionElasticService::class.qualifiedName}
            """.trimIndent()
        )
    }
}
