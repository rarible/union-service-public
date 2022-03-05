package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.dto.OwnershipSourceDto

object OwnershipSourceComparator {

    private val priority = mapOf(
        OwnershipSourceDto.TRANSFER to 10,
        OwnershipSourceDto.PURCHASE to 20,
        OwnershipSourceDto.MINT to 30,
    )

    private fun OwnershipSourceDto.getPriority(): Int {
        return priority[this]!!
    }

    fun getPreferred(current: OwnershipSourceDto?, updated: OwnershipSourceDto): OwnershipSourceDto {
        if (current == null) return updated

        return if (current.getPriority() >= updated.getPriority()) {
            current
        } else {
            updated
        }
    }

}