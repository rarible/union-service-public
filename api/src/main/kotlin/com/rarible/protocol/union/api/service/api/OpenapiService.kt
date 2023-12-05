package com.rarible.protocol.union.api.service.api

import com.rarible.protocol.union.api.UnionOpenapiReader
import com.rarible.protocol.union.api.configuration.OpenapiProperties
import com.rarible.protocol.union.core.util.OpenapiSubstitutor
import org.springframework.stereotype.Component

@Component
class OpenapiService(
    private val openapiProperties: OpenapiProperties
) {

    private val yamlCache = readYaml()
        .substituteHost()
        .substituteExamples()

    fun getOpenapiYaml(): String {
        return yamlCache
    }

    private fun readYaml(): String {
        return UnionOpenapiReader.getOpenapi()
            .bufferedReader()
            .use { it.readText() }
    }

    private fun String.substituteHost(): String {
        return OpenapiSubstitutor.substituteHost(
            this,
            openapiProperties.baseUrl,
            openapiProperties.description,
            openapiProperties.envs
        )
    }

    private fun String.substituteExamples(): String {
        return OpenapiSubstitutor.substituteExamples(
            this,
            openapiProperties.examples
        )
    }
}
