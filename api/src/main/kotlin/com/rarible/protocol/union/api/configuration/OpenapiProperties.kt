package com.rarible.protocol.union.api.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("api.openapi")
data class OpenapiProperties(
    val baseUrl: String = "",
    val description: String = "",
    // Map of examples for openapi, like "itemId" -> "ETHEREUM:0x123"
    // In openapi examples should be declared as ${itemId}
    val examples: Map<String, String> = mapOf()
)
