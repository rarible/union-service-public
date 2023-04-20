package com.rarible.protocol.union.core.model.elastic

import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import java.time.Instant

sealed class ElasticActivityFilter {
    abstract val cursor: String?
}

/**
 * ALL conditions (that aren't null/empty) must be true to pass the filter
 */
data class ElasticActivityQueryGenericFilter(
    val blockchains: Set<BlockchainDto> = emptySet(),
    val activityTypes: Set<ActivityTypeDto> = emptySet(),
    val anyUsers: Set<String> = emptySet(),
    val usersFrom: Set<String> = emptySet(),
    val usersTo: Set<String> = emptySet(),
    val collections: Set<String> = emptySet(),
    val item: String? = null,
    val from: Instant? = null,
    val to: Instant? = null,
    override val cursor: String? = null,
) : ElasticActivityFilter()

data class ActivityByCollectionFilter(
    val collections: List<CollectionIdDto>,
    val activityTypes: Set<ActivityTypeDto>,
    override val cursor: String? = null,
): ElasticActivityFilter() {

}
