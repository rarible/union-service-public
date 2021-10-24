package com.rarible.protocol.union.integration.ethereum

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@ConditionalOnProperty(name = ["integration.ethereum.enabled"], havingValue = "true")
annotation class EthereumComponent
