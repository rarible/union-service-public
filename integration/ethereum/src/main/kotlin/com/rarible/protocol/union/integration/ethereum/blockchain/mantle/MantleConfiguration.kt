package com.rarible.protocol.union.integration.ethereum.blockchain.mantle

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["integration.mantle.enabled"], havingValue = "true")
annotation class MantleConfiguration
