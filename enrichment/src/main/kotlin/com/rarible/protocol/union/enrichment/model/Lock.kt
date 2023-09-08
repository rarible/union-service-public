package com.rarible.protocol.union.enrichment.model

import com.rarible.core.common.nowMillis
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("lock")
data class Lock(
    @Id
    val id: String,
    val acquired: Boolean = false,
    val acquiredAt: Instant = nowMillis(),
    @Version
    val version: Long? = null
)
