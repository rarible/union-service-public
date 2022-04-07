package com.rarible.protocol.union.enrichment.validator

import com.rarible.protocol.union.dto.OwnershipDto
import org.slf4j.LoggerFactory

object OwnershipValidator {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun isValid(ownership: OwnershipDto?): Boolean {
        if (ownership == null) {
            return true
        }

        val bestSellValid = ownership.bestSellOrder?.let {
            val result = BestOrderValidator.isValid(it)
            if (!result) {
                logger.warn(
                    "Found Ownership [{}] with corrupted best sell Order: [{}], status = {}, taker = {}",
                    ownership.id.fullId(), it.id.fullId(), it.status, it.taker
                )
            }
            result
        } ?: true

        return bestSellValid
    }
}