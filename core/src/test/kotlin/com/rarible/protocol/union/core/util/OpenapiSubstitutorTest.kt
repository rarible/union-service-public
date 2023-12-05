package com.rarible.protocol.union.core.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OpenapiSubstitutorTest {

    @Test
    fun `substitute examples`() {
        val yaml = "itemId \${itemId} \${orderId}"
        val examples = mapOf(
            "itemId" to "123", // present in yaml, should be replaced
            "ownershipId" to "123:321" // not present in yaml
        )
        val withExamples = OpenapiSubstitutor.substituteExamples(yaml, examples)

        // there is no orderId substitution, should be kept as is
        assertThat(withExamples).isEqualTo("itemId 123 \${orderId}")
    }

    @Test
    fun `substitute host`() {
        val yaml = """
---
openapi: "3.0.1"
info:
  title: "title"
paths:
components:"""

        val expectedYaml = """
---
openapi: "3.0.1"
info:
  title: "title"
servers:
  - url: "http://{environment}.localhost.org"
    description: "test desc"
    variables:
      environment:
        enum: ["api", "test-api"]
        default: "api"
paths:
components:"""

        val withHost = OpenapiSubstitutor.substituteHost(
            yaml = yaml,
            baseUrl = "http://{environment}.localhost.org",
            description = "test desc",
            envs = listOf("prod", "test")
        )

        assertThat(withHost).isEqualTo(expectedYaml)
    }
}
