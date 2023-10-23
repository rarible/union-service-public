package com.rarible.protocol.union.core.model.elastic

import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CurrencyIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import java.time.Instant

data class ElasticActivityFilter(
    val blockchains: Set<BlockchainDto> = emptySet(),
    val activityTypes: Set<ActivityTypeDto> = emptySet(),
    val anyUsers: Set<String> = emptySet(),
    val usersFrom: Set<String> = emptySet(),
    val usersTo: Set<String> = emptySet(),
    val collections: Set<CollectionIdDto> = emptySet(),
    val bidCurrencies: Set<CurrencyIdDto> = emptySet(),
    val items: Set<ItemIdDto> = emptySet(),
    override val from: Instant? = null,
    override val to: Instant? = null,
    val cursor: String? = null,
) : DateRangeFilter<ElasticActivityFilter> {

    override fun applyDateRange(range: DateRange): ElasticActivityFilter = copy(
        from = range.from,
        to = range.to,
    )
}
