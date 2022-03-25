package com.rarible.protocol.union.api.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("api.openapi")
data class OpenapiProperties(
    val baseUrl: String = "",
    val description: String = ""
)