package com.rarible.protocol.union.enrichment.validator

import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.OrderDto
import org.slf4j.LoggerFactory

object EntityValidator {

    private val logger = LoggerFactory.getLogger(EntityValidator::class.java)

    fun isValid(item: ItemDto?): Boolean {
        if (item == null) {
            return true
        }
        return isAllValid(item.bestBidOrder, item.id.fullId())
    }

    fun isValid(collection: CollectionDto?): Boolean {
        if (collection == null) {
            return true
        }
        return isAllValid(collection.bestBidOrder, collection.id.fullId())
    }

    private fun isAllValid(order: OrderDto?, id: String): Boolean {
        return isBestBidValid(order,id) && isBestSellValid(order, id)
    }

    private fun isBestBidValid(order: OrderDto?, id: String): Boolean {
        return order?.let {
            val result = BestOrderValidator.isValid(it)
            if (!result) {
                logger.warn(
                    "Found Item [{}] with corrupted best bid Order: [{}], status = {}, taker = {}",
                    id, it.id.fullId(), it.status, it.taker
                )
            }
            result
        } ?: true
    }

    private fun isBestSellValid(order: OrderDto?, id: String): Boolean {
        return order?.let {
            val result = BestOrderValidator.isValid(it)
            if (!result) {
                logger.warn(
                    "Found Item [{}] with corrupted best sell Order: [{}], status = {}, taker = {}",
                    id, it.id.fullId(), it.status, it.taker
                )
            }
            result
        } ?: true
    }
}