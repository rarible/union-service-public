package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.BlockchainDto
import java.time.Instant

data class ElasticItemFilter(
    val blockchains: Set<BlockchainDto> = emptySet(),
    val itemIds: Set<String> = emptySet(),
    val creators: Set<String> = emptySet(),
    val owners: Set<String> = emptySet(),
    val collections: Set<String> = emptySet(),
    val mintedFrom: Instant? = null,
    val mintedTo: Instant? = null,
    val updatedFrom: Instant? = null,
    val updatedTo: Instant? = null,
    val deleted: Boolean? = null,
    val meta: String? = null,
    val cursor: String? = null,
)