package com.rarible.protocol.union.api.model.internal

import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto

data class EsMaintenanceReindexActivityDto(
    val blockchains: List<BlockchainDto> = emptyList(),
    val types: List<ActivityTypeDto> = emptyList(),
    val from: Long? = null,
    val to: Long? = null,
    val esIndex: String,
)
