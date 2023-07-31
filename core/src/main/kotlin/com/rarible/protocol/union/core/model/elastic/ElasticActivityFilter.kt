package com.rarible.protocol.union.core.model.elastic

import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CurrencyIdDto
import java.time.Instant

data class ElasticActivityFilter(
    val blockchains: Set<BlockchainDto> = emptySet(),
    val activityTypes: Set<ActivityTypeDto> = emptySet(),
    val anyUsers: Set<String> = emptySet(),
    val usersFrom: Set<String> = emptySet(),
    val usersTo: Set<String> = emptySet(),
    val collections: Set<CollectionIdDto> = emptySet(),
    val bidCurrencies: Set<CurrencyIdDto> = emptySet(),
    val item: String? = null,
    val from: Instant? = null,
    val to: Instant? = null,
    val cursor: String? = null,
)
