package com.rarible.protocol.union.integration.flow

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@ConditionalOnProperty(name = ["integration.flow.enabled"], havingValue = "true")
annotation class FlowComponent
