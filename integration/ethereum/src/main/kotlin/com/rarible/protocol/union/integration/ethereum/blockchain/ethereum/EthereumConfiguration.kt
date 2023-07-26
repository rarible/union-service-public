package com.rarible.protocol.union.integration.ethereum.blockchain.ethereum

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["integration.ethereum.enabled"], havingValue = "true")
annotation class EthereumConfiguration
