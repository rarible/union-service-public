package com.rarible.protocol.union.core.elasticsearch

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType

interface EsRepository {
    suspend fun refresh()
    fun init()
}