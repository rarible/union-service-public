package com.rarible.protocol.union.integration.immutablex

import com.rarible.protocol.union.core.DefaultClientProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "integration.immutablex")
class ImxIntegrationProperties(
    val enabled: Boolean,
    val client: ImxClientProperties?,
    val apiKey: String,
    val clientV3: ImxClientProperties?,
)

class ImxClientProperties(
    url: String,
    val byIdsChunkSize: Int = 16
) : DefaultClientProperties(url)
