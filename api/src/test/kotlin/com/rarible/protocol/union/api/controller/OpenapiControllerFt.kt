package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.client.RestTemplate

@IntegrationTest
internal class OpenapiControllerFt : AbstractIntegrationTest() {

    @Autowired
    lateinit var testTemplate: RestTemplate

    @Test
    fun `get openapi yaml`() {
        val yaml = testTemplate.getForObject("$baseUri/v0.1/openapi.yaml", String::class.java)!!

        assertTrue(yaml.contains("openapi:"))
        assertTrue(yaml.contains("paths:"))
        assertTrue(yaml.contains("/v0.1/"))
        assertTrue(yaml.contains("Item:"))
        assertTrue(yaml.contains("components:"))
        assertTrue(yaml.contains("servers:"))
        assertTrue(yaml.contains("  - url: \"https://test-api.rarible.org\""))
        assertTrue(yaml.contains("    description: \"Development (Ropsten, Mumbai, Hangzhou)\""))
    }

    @Test
    fun `get redocly html`() {
        val redocly = testTemplate.getForObject("$baseUri/v0.1/doc", String::class.java)!!

        assertTrue(redocly.contains("<html>"))
    }
}
