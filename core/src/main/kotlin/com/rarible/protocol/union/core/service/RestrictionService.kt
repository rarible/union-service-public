package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.core.restriction.RestrictionApiRuleChecker
import com.rarible.protocol.union.core.restriction.RestrictionCheckResult
import com.rarible.protocol.union.core.restriction.parameters
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipRestrictionCheckFormDto
import com.rarible.protocol.union.dto.Restriction
import com.rarible.protocol.union.dto.RestrictionApiRule
import com.rarible.protocol.union.dto.RestrictionCheckFormDto
import com.rarible.protocol.union.dto.RestrictionTypeDto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component

@Component
class RestrictionService(
    private val router: BlockchainRouter<ItemService>,
    private val restrictionApiRuleChecker: RestrictionApiRuleChecker
) {

    suspend fun checkRestriction(
        itemId: ItemIdDto,
        form: RestrictionCheckFormDto
    ): RestrictionCheckResult {
        // TODO what if meta not found?
        val meta = router.getService(itemId.blockchain)
            .getItemMetaById(itemId.value)

        val type = getType(form)
        val filteredRestrictions = meta.restrictions.filter { it.type == type }

        if (filteredRestrictions.isEmpty()) {
            return RestrictionCheckResult(true)
        }

        val responses = coroutineScope {
            filteredRestrictions.map {
                async { checkRule(itemId, it, form) }
            }.awaitAll()
        }

        return responses.reduce { first, second -> first.reduce(second) }
    }

    private suspend fun checkRule(
        itemId: ItemIdDto,
        restriction: Restriction,
        form: RestrictionCheckFormDto
    ): RestrictionCheckResult {
        // TODO UNION what if unexpected API exception?
        return when (val rule = restriction.rule) {
            is RestrictionApiRule -> restrictionApiRuleChecker.checkRule(itemId, rule, form.parameters())
        }
    }

    private fun getType(form: RestrictionCheckFormDto): RestrictionTypeDto {
        return when (form) {
            is OwnershipRestrictionCheckFormDto -> RestrictionTypeDto.OWNERSHIP
        }
    }
}