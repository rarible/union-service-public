package com.rarible.protocol.union.core.util

import org.apache.commons.text.StringSubstitutor

object OpenapiSubstitutor {

    private const val SERVER_BLOCK =
        """servers:
  - url: "%s"
    description: "%s"
paths:"""

    // TODO Ugly hack, originally should be managed by model-generator
    fun substituteHost(yaml: String, baseUrl: String, description: String): String {
        val replacement = SERVER_BLOCK.format(baseUrl, description)

        return yaml.replaceFirst("paths:", replacement)
    }

    fun substituteExamples(yaml: String, examples: Map<String, String>): String {
        val substitutor = StringSubstitutor(examples)
        return substitutor.replace(yaml)
    }

}