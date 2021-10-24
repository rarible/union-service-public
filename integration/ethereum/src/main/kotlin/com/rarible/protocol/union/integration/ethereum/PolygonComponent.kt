package com.rarible.protocol.union.integration.ethereum

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@ConditionalOnProperty(name = ["integration.polygon.enabled"], havingValue = "true")
annotation class PolygonComponent
