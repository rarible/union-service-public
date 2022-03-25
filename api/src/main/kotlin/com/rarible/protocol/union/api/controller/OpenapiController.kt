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

"""

    private val yamlCache = serversBlock +
        UnionOpenapiReader.getOpenapi().bufferedReader().use { it.readText() }

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
