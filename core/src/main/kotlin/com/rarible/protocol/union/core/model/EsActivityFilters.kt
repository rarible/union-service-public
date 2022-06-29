package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import java.time.Instant

sealed class ElasticActivityFilter

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
    val cursor: String? = null,
) : ElasticActivityFilter()

data class ActivityByCollectionFilter(
    val collections: List<CollectionIdDto>,
    val activityTypes: Set<ActivityTypeDto>,
    val cursor: String? = null
): ElasticActivityFilter() {

}

/**
 * ALL conditions (that aren't null/empty) must be true to pass the filter,
 * also additional conditions, specific to each ActivityType, are applied
 */
data class ElasticActivityQueryPerTypeFilter(
    val blockchains: Set<BlockchainDto>,
    val from: Instant?,
    val to: Instant?,
    val cursor: String?,
    val filters: Map<ActivityTypeDto, Filter>,
) : ElasticActivityFilter() {

    data class Filter(
        val makers: Set<String>,
        val takers: Set<String>,
        val makeCollection: String?,
        val takeCollection: String?,
        val makeItem: String?,
        val takeItem: String?,
    )
}

val ElasticActivityFilter.cursor
    get() = when(this) {
        is ElasticActivityQueryGenericFilter -> this.cursor
        is ElasticActivityQueryPerTypeFilter -> this.cursor
        is ActivityByCollectionFilter -> this.cursor
    }
