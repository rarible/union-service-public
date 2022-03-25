package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.UnionOpenapiReader
import com.rarible.protocol.union.api.configuration.OpenapiProperties
import org.springframework.core.io.InputStreamResource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/v0.1"])
class OpenapiController(
    private val openapiProperties: OpenapiProperties
) {

    private val serversBlock = """        
servers:
  - url: "${openapiProperties.baseUrl}"
    description: "${openapiProperties.description}"
paths:"""

    // TODO Ugly hack, originally should be managed by model-generator
    private val yamlCache =
        UnionOpenapiReader.getOpenapi().bufferedReader().use { it.readText() }
            .replaceFirst("paths:", serversBlock)

    @GetMapping(
        value = ["/openapi.yaml"],
        produces = ["text/yaml"]
    )
    fun openapiYaml(): String {
        return yamlCache
    }

    @GetMapping(
        value = ["/doc"],
        produces = ["text/html"]
    )
    fun doc(): InputStreamResource {
        val file = OpenapiController::class.java.getResourceAsStream("/redoc.html")
        return InputStreamResource(file)
    }
}
