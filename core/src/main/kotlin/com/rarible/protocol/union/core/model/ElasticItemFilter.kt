package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.BlockchainDto
import java.time.Instant

data class ElasticItemFilter(
    val blockchains: Set<String>? = null,
    val itemIds: Set<String>? = null,
    val creators: Set<String>? = null,
    val owners: Set<String> ? = null,
    val collections: Set<String> ? = null,
    val mintedFrom: Instant? = null,
    val mintedTo: Instant? = null,
    val updatedFrom: Instant? = null,
    val updatedTo: Instant? = null,
    val deleted: Boolean? = null,
    val meta: String? = null,
    val cursor: String? = null,
)