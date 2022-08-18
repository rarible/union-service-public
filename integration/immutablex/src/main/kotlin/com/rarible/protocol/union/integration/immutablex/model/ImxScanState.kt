package com.rarible.protocol.union.integration.immutablex.model

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId
import java.time.Instant

@Document("immutablex_state")
data class ImxScanState(
    @MongoId(FieldType.STRING)
    val id: String,
    val entityId: String,
    val entityDate: Instant,
    val lastDate: Instant? = null,
    val lastError: String? = null,
    val lastErrorDate: Instant? = null,
    val lastErrorStacktrace: String? = null
)
