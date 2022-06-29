package com.rarible.protocol.union.integration.immutablex.entity

import java.time.Instant
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId

@Document("immutablex_state")
data class ImmutablexState(
    @MongoId(FieldType.STRING)
    val id: String,
    val cursor: String? = null,
    @LastModifiedDate
    val lastDate: Instant? = null,
    val lastError: String? = null,
    val lastErrorStacktrace: String? = null
)
