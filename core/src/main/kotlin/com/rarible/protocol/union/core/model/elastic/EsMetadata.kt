package com.rarible.protocol.union.core.model.elastic

import org.springframework.data.annotation.Id

data class EsMetadata(
    @Id
    val id: String,
    val content: String
)
