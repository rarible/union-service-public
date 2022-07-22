package com.rarible.protocol.union.enrichment.configuration

import java.time.Duration

data class DownloaderProperties(
    val retries: List<Duration>
)