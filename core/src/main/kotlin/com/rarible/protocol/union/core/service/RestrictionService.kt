package com.rarible.protocol.union.core.service

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.core.model.Restriction
import com.rarible.protocol.union.core.model.RestrictionCheckResult
import com.rarible.protocol.union.core.model.RestrictionRule
import com.rarible.protocol.union.core.restriction.RestrictionRuleChecker
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.util.ReflectUtils
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipRestrictionCheckFormDto
import com.rarible.protocol.union.dto.RestrictionCheckFormDto
import com.rarible.protocol.union.dto.RestrictionTypeDto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class RestrictionService(
    private val router: BlockchainRouter<ItemService>,
    declaredCheckers: List<RestrictionRuleChecker<*>>
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    // We have individual checker for each subtype of RestrictionRule,
    // so here we can map it by interface generic class in order to
    // retrieve required checker using type of RestrictionRule
    private val checkersByRuleType = declaredCheckers.associateBy {
        ReflectUtils.getGenericInterfaceType(it, RestrictionRuleChecker::class.java)
    }

    suspend fun checkRestriction(
        itemId: ItemIdDto,
        form: RestrictionCheckFormDto
    ): RestrictionCheckResult {
        // TODO what if meta not found?
        val meta = try {
            router.getService(itemId.blockchain).getItemMetaById(itemId.value)
        } catch (e: WebClientResponseProxyException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                logger.warn("Failed to check restrictions for [{}], meta not found", itemId)
                return RestrictionCheckResult(true)
            } else {
                throw e
            }
        }

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

    @Suppress("UNCHECKED_CAST")
    private suspend fun checkRule(
        itemId: ItemIdDto,
        restriction: Restriction,
        form: RestrictionCheckFormDto
    ): RestrictionCheckResult {
        // TODO UNION what if unexpected API exception?
        val rule = restriction.rule
        val checker = checkersByRuleType[rule.javaClass]!! as RestrictionRuleChecker<RestrictionRule>
        return checker.checkRule(itemId, rule, form)

    }

    private fun getType(form: RestrictionCheckFormDto): RestrictionTypeDto {
        return when (form) {
            is OwnershipRestrictionCheckFormDto -> RestrictionTypeDto.OWNERSHIP
        }
    }
}