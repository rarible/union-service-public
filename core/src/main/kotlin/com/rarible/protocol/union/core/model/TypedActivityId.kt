package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.ActivityTypeDto

data class TypedActivityId(
    val id: String,
    val type: ActivityTypeDto,
)
