package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.service.api.OpenapiService
import org.springframework.core.io.InputStreamResource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/v0.1"])
class OpenapiController(
    private val openapiService: OpenapiService
) {

    @GetMapping(
        value = ["/openapi.yaml"],
        produces = ["text/yaml"]
    )
    fun openapiYaml(): String {
        return openapiService.getOpenapiYaml()
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
