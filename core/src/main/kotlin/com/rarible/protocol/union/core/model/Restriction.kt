package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.RestrictionTypeDto

data class Restriction(
    val type: RestrictionTypeDto,
    val rule: RestrictionRule
)
