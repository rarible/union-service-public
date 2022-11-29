package com.rarible.protocol.union.enrichment.configuration

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

// Enabled by default at all envs, disabled in consul for staging
@ConditionalOnProperty(name = ["clickhouse.enabled"], havingValue = "true", matchIfMissing = true)
annotation class ConditionalOnClickhouseEnabled