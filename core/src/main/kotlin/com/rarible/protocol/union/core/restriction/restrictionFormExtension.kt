package com.rarible.protocol.union.core.restriction

import com.rarible.protocol.union.dto.OwnershipRestrictionCheckFormDto
import com.rarible.protocol.union.dto.RestrictionCheckFormDto
import com.rarible.protocol.union.dto.RestrictionTypeDto

fun RestrictionCheckFormDto.type(): RestrictionTypeDto {
    return when (this) {
        is OwnershipRestrictionCheckFormDto -> RestrictionTypeDto.OWNERSHIP
    }
}

fun RestrictionCheckFormDto.parameters(): Map<String, String> {
    return when (this) {
        is OwnershipRestrictionCheckFormDto -> mapOf("user" to this.user.value)
    }
}
