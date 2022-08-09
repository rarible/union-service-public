package com.rarible.protocol.union.integration.immutablex.scanner

import java.time.Instant

class ImxScanResult(
    val cursor: String?,
    val entityDate: Instant?,
) {

    val completed = cursor.isNullOrBlank()
}