package com.rarible.protocol.union.integration.tezos

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@ConditionalOnProperty(name = ["integration.tezos.enabled"], havingValue = "true")
annotation class TezosComponent
