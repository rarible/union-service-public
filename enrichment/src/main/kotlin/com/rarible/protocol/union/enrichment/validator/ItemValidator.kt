package com.rarible.protocol.union.enrichment.validator

import com.rarible.protocol.union.dto.ItemDto
import org.slf4j.LoggerFactory

object ItemValidator {

    private val logger = LoggerFactory.getLogger(ItemValidator::class.java)

    fun isValid(item: ItemDto?): Boolean {
        if (item == null) {
            return true
        }

        val bestBidValid = item.bestBidOrder?.let {
            val result = BestOrderValidator.isValid(it)
            if (!result) {
                logger.warn(
                    "Found Item [{}] with not Active best bid order: [{}], status = {}, taker = {}",
                    item.id.fullId(), it.id.fullId(), it.status, it.taker
                )
            }
            result
        } ?: true

        val bestSellValid = item.bestSellOrder?.let {
            val result = BestOrderValidator.isValid(it)
            if (!result) {
                logger.warn(
                    "Found Item [{}] with not Active best sell order: [{}], status = {}, taker = {}",
                    item.id.fullId(), it.id.fullId(), it.status, it.taker
                )
            }
            result
        } ?: true

        return bestSellValid && bestBidValid
    }

}