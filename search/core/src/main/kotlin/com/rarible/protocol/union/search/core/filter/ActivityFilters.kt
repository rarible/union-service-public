package com.rarible.protocol.union.search.core.filter

import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import java.time.Instant

sealed class ElasticActivityFilter

/**
 * ALL conditions (that aren't null/empty) must be true to pass the filter
 */
data class ElasticActivityQueryGenericFilter(
    val blockchains: Set<BlockchainDto>,
    val activityTypes: Set<ActivityTypeDto>,
    val anyUsers: Set<String>,
    val makers: Set<String>,
    val takers: Set<String>,
    val anyCollections: Set<String>,
    val makeCollections: Set<String>,
    val takeCollections: Set<String>,
    val anyItems: Set<String>,
    val makeItems: Set<String>,
    val takeItems: Set<String>,
    val from: Instant?,
    val to: Instant?,
    val cursor: String?,
) : ElasticActivityFilter()

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
