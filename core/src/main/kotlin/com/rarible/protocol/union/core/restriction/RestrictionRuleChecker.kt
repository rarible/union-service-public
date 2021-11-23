package com.rarible.protocol.union.core.restriction

import com.rarible.protocol.union.core.model.RestrictionCheckResult
import com.rarible.protocol.union.core.model.RestrictionRule
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.RestrictionCheckFormDto

interface RestrictionRuleChecker<T : RestrictionRule> {

    suspend fun checkRule(
        itemId: ItemIdDto,
        rule: T,
        form: RestrictionCheckFormDto
    ): RestrictionCheckResult

}