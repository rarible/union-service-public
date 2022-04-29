package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.BlockchainDto
import java.time.Instant

data class EsItemFilter(
    val blockchain: BlockchainDto? = null,
    val itemId: String? = null,
    val creator: String? = null,
    val owner: String? = null,
    val collection: String? = null,
    val mintedAtFrom: Instant? = null,
    val mintedAtTo: Instant? = null,
    val lastUpdatedAtFrom: Instant? = null,
    val lastUpdatedAtTo: Instant? = null,
    val deleted: Boolean = false,
)
