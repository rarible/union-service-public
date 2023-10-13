package com.rarible.protocol.union.integration.flow

import com.rarible.protocol.union.core.DefaultConsumerProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "integration.flow")
class FlowIntegrationProperties(
    val enabled: Boolean,
    val consumer: DefaultConsumerProperties?
)
