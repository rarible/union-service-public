package com.rarible.protocol.union.search.core

import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import java.time.Instant

data class ElasticActivity(
    val activityId: String,
    // Sort fields
    val date: Instant,
    val blockNumber: Long?,
    val logIndex: Int?,
    // Filter fields
    val blockchain: BlockchainDto,
    val type: ActivityTypeDto,
    val user: User,
    val collection: Collection,
    val item: Item,

) {
    data class User(
        val maker: String,
        val taker: String?,
    )

    data class Collection(
        val make: String,
        val take: String?
    )

    data class Item(
        val make: String,
        val take: String?
    )
}
