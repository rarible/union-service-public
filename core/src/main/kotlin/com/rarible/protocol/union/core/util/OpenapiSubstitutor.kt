package com.rarible.protocol.union.core.util

import org.apache.commons.text.StringSubstitutor

object OpenapiSubstitutor {

    private const val SERVER_BLOCK =
        """servers:
  - url: "%s"
    description: "%s"
    variables:
      environment:
        enum: [%s]
        default: %s
paths:"""

    // TODO Ugly hack, originally should be managed by model-generator
    fun substituteHost(yaml: String, baseUrl: String, description: String, envs: List<String>): String {
        val envEnum = envs.map { "\"${generateHost(it)}\"" }
        val defaultEnv = envEnum.firstOrNull() ?: ""
        val replacement = SERVER_BLOCK.format(
            baseUrl,
            description,
            envEnum.joinToString(),
            defaultEnv
        )

        return yaml.replaceFirst("paths:", replacement)
    }

    private fun generateHost(env: String): String {
        return if (env == "prod") {
            "api"
        } else {
            "$env-api"
        }
    }

    fun substituteExamples(yaml: String, examples: Map<String, String>): String {
        val substitutor = StringSubstitutor(examples)
        return substitutor.replace(yaml)
    }
}
