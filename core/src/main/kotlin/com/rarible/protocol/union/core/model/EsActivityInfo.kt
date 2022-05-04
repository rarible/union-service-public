package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import java.time.Instant

data class EsActivityInfo(
    val activityId: String, // blockchain:value
    val blockchain: BlockchainDto,
    val type: ActivityTypeDto,

    // TODO: replace with single cursor field?
    val date: Instant,
    val blockNumber: Long?,
    val logIndex: Int?,
    val salt: Long,
)
